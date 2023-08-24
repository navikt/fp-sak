package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.*;

/**
 * Marker type som implementerer interface {@link Dokumentmottaker}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface DokumentGruppeRef {

    /**
     * Settes til navn på dokumentgruppe slik det defineres i KODELISTE-tabellen.
     */
    DokumentGruppe value();

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class DokumentGruppeRefLiteral extends AnnotationLiteral<DokumentGruppeRef> implements DokumentGruppeRef {

        private DokumentGruppe gruppe;

        DokumentGruppeRefLiteral(DokumentGruppe gruppe) {
            this.gruppe = gruppe;
        }

        @Override
        public DokumentGruppe value() {
            return gruppe;
        }
    }
}
