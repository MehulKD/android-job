package com.evernote.android.job.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.vrallev.android.cat.print.CatPrinter;

import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author rwondratschek
 */
@FixMethodOrder(MethodSorters.JVM)
public class LoggerTest {

    private boolean mResetValueCalled;

    @After
    public void resetValue() {
        JobCat.setLogcatEnabled(true);
        mResetValueCalled = true;
    }

    @Test
    public void testIsLogcatEnabled() {
        // first test in class, so resetValue() hasn't been called, yet
        assertThat(mResetValueCalled).isFalse();
        assertThat(JobCat.isLogcatEnabled()).isTrue();

        JobCat.setLogcatEnabled(false);
        assertThat(JobCat.isLogcatEnabled()).isFalse();
    }

    @Test
    public void testAddIsIdempotent() {
        TestPrinter printer = new TestPrinter();
        assertThat(JobCat.addLogPrinter(printer)).isTrue();
        assertThat(JobCat.addLogPrinter(printer)).isFalse();
    }

    @Test
    public void testRemove() {
        TestPrinter printer = new TestPrinter();
        assertThat(JobCat.addLogPrinter(printer)).isTrue();
        JobCat.removeLogPrinter(printer);
        assertThat(JobCat.addLogPrinter(printer)).isTrue();
    }

    @Test
    public void testSingleCustomLoggerAddBefore() {
        TestPrinter printer = new TestPrinter();
        assertThat(JobCat.addLogPrinter(printer)).isTrue();

        JobCat cat = new JobCat("Tag");
        cat.d("hello");
        cat.w("world");

        assertThat(printer.mMessages).contains("hello", "world");
    }

    @Test
    public void testSingleCustomLoggerAddAfter() {
        JobCat cat = new JobCat("Tag");

        TestPrinter printer = new TestPrinter();
        assertThat(JobCat.addLogPrinter(printer)).isTrue();

        cat.d("hello");
        cat.w("world");

        assertThat(printer.mMessages).containsExactly("hello", "world");
    }

    @Test
    public void test100Loggers() {
        JobCat cat1 = new JobCat("Tag1");

        List<TestPrinter> printers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TestPrinter printer = new TestPrinter();
            assertThat(JobCat.addLogPrinter(printer)).isTrue();
            printers.add(printer);
        }

        JobCat cat2 = new JobCat("Tag2");

        cat1.d("hello");
        cat2.w("world");

        for (TestPrinter printer : printers) {
            assertThat(printer.mTags).containsExactly("Tag1", "Tag2");
            assertThat(printer.mMessages).containsExactly("hello", "world");
        }

        TestPrinter removedPrinter = printers.remove(50);
        JobCat.removeLogPrinter(removedPrinter);

        cat1.d("third");
        for (TestPrinter printer : printers) {
            assertThat(printer.mTags).containsExactly("Tag1", "Tag2", "Tag1");
            assertThat(printer.mMessages).containsExactly("hello", "world", "third");
        }
        assertThat(removedPrinter.mTags).containsExactly("Tag1", "Tag2");
        assertThat(removedPrinter.mMessages).containsExactly("hello", "world");
    }

    @Test
    public void testNotVerboseLogging() {
        JobCat cat = new JobCat("Tag");

        TestPrinter fakeLogcatPrinter = new TestPrinter();
        cat.addPrinter(fakeLogcatPrinter); // in this list logcat is enabled

        cat.d("hello");

        assertThat(fakeLogcatPrinter.mMessages).containsExactly("hello");

        JobCat.setLogcatEnabled(false);

        cat.d("world");
        assertThat(fakeLogcatPrinter.mMessages).containsExactly("hello");
    }

    private static final class TestPrinter implements CatPrinter {

        private final List<String> mTags = new ArrayList<>();
        private final List<String> mMessages = new ArrayList<>();

        @Override
        public void println(int priority, @NonNull String tag, @NonNull String message, @Nullable Throwable t) {
            mTags.add(tag);
            mMessages.add(message);
        }
    }
}
