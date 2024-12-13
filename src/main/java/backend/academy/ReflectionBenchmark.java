package backend.academy;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Thread)
@SuppressWarnings({"MagicNumber", "UncommentedMain", "MultipleStringLiterals"})
public class ReflectionBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(ReflectionBenchmark.class.getSimpleName())
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.NANOSECONDS)
            .forks(1)
            .warmupForks(1)
            .warmupIterations(1)
            .warmupTime(TimeValue.seconds(5))
            .measurementIterations(10)
            .measurementTime(TimeValue.seconds(5))
            .build();

        new Runner(options).run();
    }

    backend.academy.Student student;
    Method method;
    MethodHandle nameMH;
    Function<Student, String> getNameFunction;

    @Setup
    public void setup() throws Throwable {
        student = new backend.academy.Student("Dmitrii", "Karaulov");

        method = backend.academy.Student.class.getMethod("name");

        MethodType mt = MethodType.methodType(String.class);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        nameMH = lookup.findVirtual(backend.academy.Student.class, "name", mt);

        CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "apply",
            MethodType.methodType(Function.class),
            MethodType.methodType(Object.class, Object.class),
            nameMH,
            MethodType.methodType(String.class, Student.class)
        );
        getNameFunction = (Function<Student, String>) callSite.getTarget().invoke();
    }

    @Benchmark
    public void directAccess(Blackhole bh) {
        String name = student.name();
        bh.consume(name);
    }

    @Benchmark
    public void reflectionAccess(Blackhole bh)
        throws InvocationTargetException, IllegalAccessException {
        String name = (String) method.invoke(student);
        bh.consume(name);
    }

    @Benchmark
    public void methodHandlesAccess(Blackhole bh) throws Throwable {
        String name = (String) nameMH.invoke(student);
        bh.consume(name);
    }

    @Benchmark
    public void metafactoryAccess(Blackhole bh) {
        String name = getNameFunction.apply(student);
        bh.consume(name);
    }
}
