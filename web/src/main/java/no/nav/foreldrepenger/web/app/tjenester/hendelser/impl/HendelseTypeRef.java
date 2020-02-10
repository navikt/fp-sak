package no.nav.foreldrepenger.web.app.tjenester.hendelser.impl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.foreldrepenger.kontrakter.abonnent.HendelseDto;

@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface HendelseTypeRef {

    String FØDSEL = "FØDSEL";
    String DØD = "DØD";
    String DØDFØDSEL = "DØDFØDSEL";
    String INFOTRYGD = "INFOTRYGD";

    /**
     * Settes til navn på hendelsen slik det er definert i {@link HendelseDto}
     */
    String value();

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    class HendelseTypeRefLiteral extends AnnotationLiteral<HendelseTypeRef> implements HendelseTypeRef {

        private static final Set<String> INFOTRYGD_TYPER = Set.of("YTELSE_INNVILGET", "YTELSE_ENDRET", "YTELSE_OPPHØRT", "YTELSE_ANNULERT");
        private String type;

        public HendelseTypeRefLiteral(HendelseDto hendelseDto) {
            if (!INFOTRYGD_TYPER.contains(hendelseDto.getHendelsetype())) {
                this.type = hendelseDto.getHendelsetype();
            } else {
                this.type = INFOTRYGD;
            }
        }

        @Override
        public String value() {
            return type;
        }
    }
}
