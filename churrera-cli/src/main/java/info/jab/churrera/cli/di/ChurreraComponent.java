package info.jab.churrera.cli.di;

import info.jab.churrera.cli.ChurreraCLI;
import info.jab.churrera.cli.command.run.RunCommand;
import info.jab.churrera.cli.service.JobProcessor;
import dagger.Component;

import javax.inject.Singleton;

/**
 * Dagger component for Churrera CLI application.
 * Provides dependency injection for the entire application.
 */
@Component(modules = {
    ChurreraModule.class,
    CursorClientModule.class,
    ServiceModule.class,
    CommandModule.class
})
@Singleton
public interface ChurreraComponent {

    /**
     * Provides the main ChurreraCLI instance.
     */
    ChurreraCLI churreraCLI();

    /**
     * Provides the RunCommand instance.
     */
    RunCommand runCommand();

    /**
     * Provides the JobProcessor instance.
     */
    JobProcessor jobProcessor();
}
