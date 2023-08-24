package no.nav.foreldrepenger.mottak.hendelser;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;

import java.lang.annotation.*;

/**
 * Marker type som implementerer interface {@link ForretningshendelseHåndterer}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface ForretningshendelsestypeRef {

    /**
     * Settes til navn på forretningshendelse slik det defineres i KODELISTE-tabellen, eller til YTELSE_HENDELSE
     */
    ForretningshendelseType value();

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    class ForretningshendelsestypeRefLiteral extends AnnotationLiteral<ForretningshendelsestypeRef> implements ForretningshendelsestypeRef {

        private ForretningshendelseType hendelseType;

        public ForretningshendelsestypeRefLiteral(ForretningshendelseType forretningshendelseType) {
            this.hendelseType = forretningshendelseType;

        }

        @Override
        public ForretningshendelseType value() {
            return hendelseType;
        }
    }
}

