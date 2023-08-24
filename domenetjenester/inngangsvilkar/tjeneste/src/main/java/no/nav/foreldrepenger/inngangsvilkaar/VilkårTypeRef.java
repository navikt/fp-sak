package no.nav.foreldrepenger.inngangsvilkaar;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

import java.lang.annotation.*;

/**
 * Marker type som implementerer interface {@link Inngangsvilkår}.
 * Brukes for å konfigurere implementasjon av hvilke vilkår som skal kjøres.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface VilkårTypeRef {

    /**
     * Settes til navn på vilkår slik det defineres i VILKÅR_TYPE tabellen.
     */
    VilkårType value();

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class VilkårTypeRefLiteral extends AnnotationLiteral<VilkårTypeRef> implements VilkårTypeRef {

        private VilkårType vilkårType;

        public VilkårTypeRefLiteral(VilkårType vilkårType) {
            this.vilkårType = vilkårType;
        }

        @Override
        public VilkårType value() {
            return vilkårType;
        }

    }

}
