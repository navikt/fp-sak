package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Marker type som implementerer interface {@link Dokumentmottaker}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface DokumentGruppeRef {

    /**
     * Settes til navn på dokumentgruppe slik det defineres i KODELISTE-tabellen.
     */
    DokumentGruppe value();

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
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
