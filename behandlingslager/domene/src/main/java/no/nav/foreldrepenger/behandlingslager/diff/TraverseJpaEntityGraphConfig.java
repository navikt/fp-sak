package no.nav.foreldrepenger.behandlingslager.diff;

import java.lang.reflect.Field;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.Version;

import javassist.Modifier;

/** Konfig for å scanne JPA klasser. */
public class TraverseJpaEntityGraphConfig extends TraverseGraphConfig {

    @Override
    public boolean isMappedField(Field fld) {
        return isExpectedField(fld) && !isSkippedFields(fld);
    }

    protected boolean isExpectedField(Field fld) {
        int mods = fld.getModifiers();
        if (Modifier.isFinal(mods) || Modifier.isStatic(mods) || Modifier.isTransient(mods) || Modifier.isVolatile(mods)) {
            // her kan final felter skippes, da disse må være i løvnøder
            return false;
        }

        // følger bare standard, mappede felter i Entity grafen
        return fld.isAnnotationPresent(Column.class)
            || fld.isAnnotationPresent(JoinColumn.class)
            || fld.isAnnotationPresent(OneToOne.class)
            || fld.isAnnotationPresent(ManyToOne.class)
            || fld.isAnnotationPresent(OneToMany.class)
            || fld.isAnnotationPresent(ManyToMany.class)
            || fld.isAnnotationPresent(Embedded.class);
    }

    protected boolean isSkippedFields(Field fld) {
        return fld.isAnnotationPresent(DiffIgnore.class)
            || (fld.isAnnotationPresent(Id.class) && fld.isAnnotationPresent(GeneratedValue.class))
            || fld.isAnnotationPresent(Version.class)
            || fld.isAnnotationPresent(GeneratedValue.class)
            || fld.isAnnotationPresent(Transient.class);
    }

    @Override
    public void valider(Node currentPath, Class<?> targetClass) {
        super.valider(currentPath, targetClass);

        boolean ok = targetClass.isAnnotationPresent(Entity.class)
            || targetClass.isAnnotationPresent(Embeddable.class);
        if (!ok) {
            throw new IllegalArgumentException(
                "target [" + targetClass + "] er ikke en Entity eller Embeddable (mangler annotation):" + currentPath); //$NON-NLS-1$ //$NON-NLS-2$
        }

    }

}
