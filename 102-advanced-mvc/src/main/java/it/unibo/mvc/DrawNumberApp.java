package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final String MINIMUM = "minimum";
    private static final String MAXIMUM = "maximum";
    private static final String CASE_ATTEMPTS = "attempts";
    private static final int ARGUMENTS_NUMBER = 2; 

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param file
     * @param views
     */
    public DrawNumberApp(final String file, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        final Configuration.Builder confBuilder = new Configuration.Builder();
        try (
            BufferedReader inputFile = new BufferedReader(new InputStreamReader(
                ClassLoader.getSystemResourceAsStream(file), StandardCharsets.UTF_8))
        ) {
            String line = inputFile.readLine();
            while (Objects.nonNull(line)) {
                final StringTokenizer tokenizer = new StringTokenizer(line, ":");
                if (tokenizer.countTokens() == ARGUMENTS_NUMBER) {
                    final var valueType = tokenizer.nextToken().trim(); 
                    final var value = tokenizer.nextToken().trim();
                    switch (valueType) {
                        case MINIMUM: 
                            confBuilder.setMin(Integer.parseInt(value));
                            break; 
                        case MAXIMUM: 
                            confBuilder.setMax(Integer.parseInt(value));
                            break; 
                        case CASE_ATTEMPTS:
                            confBuilder.setAttempts(Integer.parseInt(value));
                            break; 
                        default:
                            displayError("Unexpected strings in reading");
                            break; 
                    }
                }
                line = inputFile.readLine();
            }
        } catch (final IOException e) {
            displayError(e.getMessage());
        }
        final Configuration configuration = confBuilder.build(); 
        if (configuration.isConsistent()) {
            this.model = new DrawNumberImpl(configuration); 
        } else {
            displayError("The inizialization values are not valid: "
                + "min: " + configuration.getMin() + ", "
                + "max: " + configuration.getMax() + ", "
                + "attempts: " + configuration.getAttempts() + ". The default ones will be used"
            );
            this.model = new DrawNumberImpl(new Configuration.Builder().build());
        }
    }

    private void displayError(final String err) {
        for (final DrawNumberView view: views) {
            view.displayError(err);
        }
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.displayError(e.getMessage());
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    @SuppressFBWarnings(
        value = { "DM_EXIT" },
        justification = "Excercise is designed in this way"
    )
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp("config.yml", 
                new DrawNumberViewImpl(),
                new DrawNumberViewImpl(),
                new PrintStreamView(System.out),
                new PrintStreamView("output.txt"));
    }

}
