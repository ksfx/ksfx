package ch.ksfx.services.publishing;

import ch.ksfx.services.ServiceProvider;

import java.util.List;

/**
 * The constructors a publishing strategy (configuration or resource) may have - the same two
 * PublicationViewerController.loadPublishingStrategy tries at run time: {@code (ServiceProvider,
 * List<String> uriParameters)} first, else {@code (ServiceProvider)}. Validation (MVC forms and
 * API) uses this, so it accepts exactly what runs - before, it only accepted the single-argument
 * one and rejected the demo templates, which use the URI-parameter variant.
 */
public final class PublishingStrategyConstructors
{
    private PublishingStrategyConstructors()
    {
    }

    /** Throws NoSuchMethodException naming both accepted signatures when the class has neither. */
    public static void requireSupported(Class<?> clazz) throws NoSuchMethodException
    {
        try {
            clazz.getDeclaredConstructor(ServiceProvider.class, List.class);
        } catch (NoSuchMethodException e) {
            try {
                clazz.getDeclaredConstructor(ServiceProvider.class);
            } catch (NoSuchMethodException e2) {
                throw new NoSuchMethodException(clazz.getName() + " needs a constructor (ServiceProvider serviceProvider, List<String> uriParameters) or (ServiceProvider serviceProvider)");
            }
        }
    }
}
