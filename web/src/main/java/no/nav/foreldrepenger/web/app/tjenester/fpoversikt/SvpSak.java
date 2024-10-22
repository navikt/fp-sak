package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

record SvpSak(String saksnummer,
              String aktørId,
              FamilieHendelse familieHendelse,
              boolean avsluttet,
              Set<Aksjonspunkt> aksjonspunkt,
              Set<Søknad> søknader,
              Set<Vedtak> vedtak) implements Sak {

    record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt, Set<Tilrettelegging> tilrettelegginger) {

        public record Tilrettelegging(Aktivitet aktivitet,
                                      LocalDate behovFom,
                                      String risikoFaktorer,
                                      String tiltak,
                                      Set<Periode> perioder,
                                      Set<OppholdPeriode> oppholdsperioder
        ) {
            public record Periode(LocalDate fom, TilretteleggingType type, BigDecimal arbeidstidprosent) {
            }
        }
    }
    public record OppholdPeriode(LocalDate fom, LocalDate tom, Årsak årsak, OppholdKilde kilde) {

        public enum Årsak {
            FERIE,
            SYKEPENGER
        }

        public enum OppholdKilde {
            SØKNAD,
            SAKSBEHANDLER,
            INNTEKTSMELDING
        }
    }
    public record Vedtak(LocalDateTime vedtakstidspunkt, Set<ArbeidsforholdUttak> arbeidsforhold, AvslagÅrsak avslagÅrsak) {



        public record ArbeidsforholdUttak(Aktivitet aktivitet,
                                          LocalDate behovFom,
                                          String risikoFaktorer,
                                          String tiltak,
                                          Set<SvpPeriode> svpPerioder,
                                          Set<OppholdPeriode> oppholdsperioder,
                                          ArbeidsforholdIkkeOppfyltÅrsak ikkeOppfyltÅrsak
        ) {
            public record SvpPeriode(LocalDate fom,
                                     LocalDate tom,
                                     TilretteleggingType tilretteleggingType,
                                     BigDecimal arbeidstidprosent,
                                     BigDecimal utbetalingsgrad,
                                     ResultatÅrsak resultatÅrsak) {

                public enum ResultatÅrsak {
                    INNVILGET,
                    AVSLAG_SØKNADSFRIST,
                    AVSLAG_ANNET,
                    AVSLAG_INNGANGSVILKÅR,
                    OPPHØR_OVERGANG_FORELDREPENGER,
                    OPPHØR_FØDSEL,
                    OPPHØR_TIDSPERIODE_FØR_TERMIN,
                    OPPHØR_OPPHOLD_I_YTELSEN,
                    OPPHØR_ANNET
                }
            }

            public enum ArbeidsforholdIkkeOppfyltÅrsak {
                ARBEIDSGIVER_KAN_TILRETTELEGGE,
                ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN,
                ANNET
            }
        }

        public enum AvslagÅrsak {
            ARBEIDSGIVER_KAN_TILRETTELEGGE,
            SØKER_ER_INNVILGET_SYKEPENGER,
            MANGLENDE_DOKUMENTASJON,
            ANNET,
        }
    }

    public record Aktivitet(Type type, Arbeidsgiver arbeidsgiver, String arbeidsforholdId) {

        public enum Type {
            ORDINÆRT_ARBEID,
            SELVSTENDIG_NÆRINGSDRIVENDE,
            FRILANS
        }
    }

    public enum TilretteleggingType {
        HEL,
        DELVIS,
        INGEN
    }

    @Override
    public String toString() {
        return "SvpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", avsluttet=" + avsluttet
            + ", aksjonspunkt=" + aksjonspunkt + ", søknader=" + søknader + ", vedtak=" + vedtak + '}';
    }
}
