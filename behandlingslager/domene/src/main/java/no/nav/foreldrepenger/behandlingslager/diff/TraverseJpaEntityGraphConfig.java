package no.nav.foreldrepenger.behandlingslager.diff;

import java.lang.reflect.Field;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import javassist.Modifier;

/**
 * Konfig for å scanne JPA klasser.
 */
public class TraverseJpaEntityGraphConfig extends TraverseGraphConfig {

    @Override
    public boolean isMappedField(Field fld) {
        return isExpectedField(fld) && !isSkippedFields(fld);
    }

    protected boolean isExpectedField(Field fld) {
        var mods = fld.getModifiers();
        if (Modifier.isFinal(mods) || Modifier.isStatic(mods) || Modifier.isTransient(mods) || Modifier.isVolatile(mods)) {
            // her kan final felter skippes, da disse må være i løvnøder
            return false;
        }

        // følger bare standard, mappede felter i Entity grafen
        return fld.isAnnotationPresent(Column.class) || fld.isAnnotationPresent(JoinColumn.class) || fld.isAnnotationPresent(OneToOne.class)
            || fld.isAnnotationPresent(ManyToOne.class) || fld.isAnnotationPresent(OneToMany.class) || fld.isAnnotationPresent(ManyToMany.class)
            || fld.isAnnotationPresent(Embedded.class);
    }

    protected boolean isSkippedFields(Field fld) {
        return fld.isAnnotationPresent(DiffIgnore.class) || fld.isAnnotationPresent(Id.class) && fld.isAnnotationPresent(GeneratedValue.class)
            || fld.isAnnotationPresent(Version.class) || fld.isAnnotationPresent(GeneratedValue.class) || fld.isAnnotationPresent(Transient.class);
    }

    @Override
    public void valider(Node currentPath, Class<?> targetClass) {
        super.valider(currentPath, targetClass);

        var ok = targetClass.isAnnotationPresent(Entity.class) || targetClass.isAnnotationPresent(Embeddable.class);
        if (!ok) {
            throw new IllegalArgumentException("target [" + targetClass + "] er ikke en Entity eller Embeddable (mangler annotation):" + currentPath);
        }

    }

}
