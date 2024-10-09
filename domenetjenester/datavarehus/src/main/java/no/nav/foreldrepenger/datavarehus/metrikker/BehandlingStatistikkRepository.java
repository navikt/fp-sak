package no.nav.foreldrepenger.datavarehus.metrikker;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summarizingLong;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class BehandlingStatistikkRepository {

    private EntityManager entityManager;

    BehandlingStatistikkRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingStatistikkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<BehandlingStatistikk> hentAntallBehandlinger() {
        var query = entityManager.createQuery("""
            select f.ytelseType, b.behandlingType, ba.behandlingÅrsakType, count(1) as antall from Fagsak f
            join Behandling b on b.fagsak.id = f.id
            left outer join BehandlingÅrsak ba on ba.behandling.id = b.id
            group by f.ytelseType, b.behandlingType, ba.behandlingÅrsakType
            """, BehandlingStatistikkEntitet.class)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        var behandlingStatistikkEntitet = query.getResultList();
        return mapTilBehandlingStatistikk(behandlingStatistikkEntitet);
    }

    public static List<BehandlingStatistikk> mapTilBehandlingStatistikk(List<BehandlingStatistikkEntitet> behandlingStatistikkEntitet) {
        return behandlingStatistikkEntitet.stream()
            .map(BehandlingStatistikkRepository::tilBehandlingStatistikk)
            .collect(groupingBy(BehandlingStatistikk::ytelseType, groupingBy(BehandlingStatistikk::behandlingType,
                groupingBy(BehandlingStatistikk::behandlingsårsak, summarizingLong(BehandlingStatistikk::antall)))))
            .entrySet()
            .stream()
            .flatMap(ytelseTypeEntry -> ytelseTypeEntry.getValue()
                .entrySet()
                .stream()
                .flatMap(behandlingTypeEntry -> behandlingTypeEntry.getValue()
                    .entrySet()
                    .stream()
                    .map(behandlingårsakEntry -> new BehandlingStatistikk(ytelseTypeEntry.getKey(), behandlingTypeEntry.getKey(),
                        behandlingårsakEntry.getKey(), behandlingårsakEntry.getValue().getSum()))))
            .toList();
    }

    private static BehandlingStatistikk tilBehandlingStatistikk(BehandlingStatistikkEntitet entitet) {
        return new BehandlingStatistikk(entitet.ytelseType(), entitet.behandlingType(), tilBehandlingÅrsak(entitet.behandlingType(), entitet.behandlingArsakType()), entitet.antall());
    }

    private static Behandlingsårsak tilBehandlingÅrsak(BehandlingType behandlingType, BehandlingÅrsakType behandlingÅrsakType) {
        return switch (behandlingÅrsakType) {
            case RE_FEIL_I_LOVANDVENDELSE,
                 RE_FEIL_REGELVERKSFORSTÅELSE,
                 RE_FEIL_ELLER_ENDRET_FAKTA,
                 RE_FEIL_PROSESSUELL,
                 RE_ANNET,
                 RE_OPPLYSNINGER_OM_MEDLEMSKAP,
                 RE_OPPLYSNINGER_OM_OPPTJENING,
                 RE_OPPLYSNINGER_OM_FORDELING,
                 RE_OPPLYSNINGER_OM_INNTEKT,
                 RE_OPPLYSNINGER_OM_FØDSEL,
                 RE_OPPLYSNINGER_OM_DØD,
                 RE_OPPLYSNINGER_OM_SØKERS_REL,
                 RE_OPPLYSNINGER_OM_SØKNAD_FRIST,
                 RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG -> Behandlingsårsak.MANUELL;
            case RE_KLAGE_UTEN_END_INNTEKT, RE_KLAGE_MED_END_INNTEKT, ETTER_KLAGE -> Behandlingsårsak.KLAGE_OMGJØRING;
            case KLAGE_TILBAKEBETALING -> Behandlingsårsak.KLAGE_TILBAKEBETALING;
            case RE_MANGLER_FØDSEL, RE_MANGLER_FØDSEL_I_PERIODE, RE_AVVIK_ANTALL_BARN -> Behandlingsårsak.ETTERKONTROLL;
            case RE_ENDRING_FRA_BRUKER -> Behandlingsårsak.SØKNAD;
            case RE_ENDRET_INNTEKTSMELDING -> Behandlingsårsak.INNTEKTSMELDING;
            case BERØRT_BEHANDLING, REBEREGN_FERIEPENGER, ENDRE_DEKNINGSGRAD -> Behandlingsårsak.BERØRT;
            case RE_UTSATT_START -> Behandlingsårsak.UTSATT_START;
            case RE_SATS_REGULERING -> Behandlingsårsak.REGULERING;
            case INFOBREV_BEHANDLING, INFOBREV_OPPHOLD, INFOBREV_PÅMINNELSE -> Behandlingsårsak.INFOBREV;
            case RE_HENDELSE_FØDSEL,
                 RE_HENDELSE_DØD_FORELDER,
                 RE_HENDELSE_DØD_BARN,
                 RE_HENDELSE_DØDFØDSEL,
                 RE_HENDELSE_UTFLYTTING -> Behandlingsårsak.FOLKEREGISTER;
            case RE_VEDTAK_PLEIEPENGER -> Behandlingsårsak.PLEIEPENGER;
            case UDEFINERT -> switch (behandlingType) {
                case FØRSTEGANGSSØKNAD -> Behandlingsårsak.SØKNAD;
                case KLAGE, ANKE -> Behandlingsårsak.KLAGE_OMGJØRING;
                default -> Behandlingsårsak.ANNET;
            };
            case null -> switch (behandlingType) {
                case FØRSTEGANGSSØKNAD -> Behandlingsårsak.SØKNAD;
                case KLAGE, ANKE -> Behandlingsårsak.KLAGE_OMGJØRING;
                default -> Behandlingsårsak.ANNET;
            };
            default -> Behandlingsårsak.ANNET;
        };
    }

    public record BehandlingStatistikkEntitet(FagsakYtelseType ytelseType,
                                              BehandlingType behandlingType,
                                              BehandlingÅrsakType behandlingArsakType,
                                              Long antall) {
    }


    public record BehandlingStatistikk(FagsakYtelseType ytelseType,
                                       BehandlingType behandlingType,
                                       Behandlingsårsak behandlingsårsak,
                                       Long antall) {
    }

    public enum Behandlingsårsak {
        SØKNAD,
        INNTEKTSMELDING,
        FOLKEREGISTER,
        PLEIEPENGER,
        ETTERKONTROLL,
        MANUELL,
        BERØRT,
        UTSATT_START,
        REGULERING,
        KLAGE_OMGJØRING,
        KLAGE_TILBAKEBETALING,
        INFOBREV,
        ANNET
    }
}
