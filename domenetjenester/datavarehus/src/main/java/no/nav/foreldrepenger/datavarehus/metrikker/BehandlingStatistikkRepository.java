package no.nav.foreldrepenger.datavarehus.metrikker;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summarizingLong;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;

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

    public List<BehandlingStatistikk> hentAntallBehandlingsårsaker() {
        var query = entityManager.createQuery("""
            select ba.behandlingÅrsakType, count(1) as antall
            from BehandlingÅrsak ba
            group by ba.behandlingÅrsakType
            """, BehandlingÅrsakQR.class)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        var behandlingStatistikkEntitet = query.getResultList();
        return mapTilBehandlingStatistikk(behandlingStatistikkEntitet);
    }

    static List<BehandlingStatistikk> mapTilBehandlingStatistikk(List<BehandlingÅrsakQR> behandlingStatistikkEntitet) {
        return behandlingStatistikkEntitet.stream()
            .map(BehandlingStatistikkRepository::tilBehandlingStatistikk)
            .collect(groupingBy(bs -> Optional.ofNullable(bs.behandlingsårsak()).orElse(Behandlingsårsak.ANNET),
                summarizingLong(bs -> Optional.ofNullable(bs.antall()).orElse(0L))))
            .entrySet()
            .stream()
            .map(behandlingårsakEntry -> new BehandlingStatistikk(behandlingårsakEntry.getKey(), behandlingårsakEntry.getValue().getSum()))
            .toList();
    }

    private static BehandlingStatistikk tilBehandlingStatistikk(BehandlingÅrsakQR entitet) {
        return new BehandlingStatistikk(tilBehandlingÅrsak(entitet.behandlingArsakType()), entitet.antall());
    }

    private static Behandlingsårsak tilBehandlingÅrsak(BehandlingÅrsakType behandlingÅrsakType) {
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
            case KLAGE_TILBAKEBETALING -> Behandlingsårsak.KLAGE_ANKE;
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
            case OPPHØR_YTELSE_NYTT_BARN -> Behandlingsårsak.NESTESAK;
            case null -> Behandlingsårsak.ANNET;
            default -> Behandlingsårsak.ANNET;
        };
    }

    record BehandlingÅrsakQR(BehandlingÅrsakType behandlingArsakType, Long antall) { }


    public record BehandlingStatistikk(Behandlingsårsak behandlingsårsak, Long antall) { }

    public enum Behandlingsårsak {
        SØKNAD,
        KLAGE_ANKE,
        INNTEKTSMELDING,
        FOLKEREGISTER,
        PLEIEPENGER,
        ETTERKONTROLL,
        MANUELL,
        BERØRT,
        UTSATT_START,
        REGULERING,
        KLAGE_OMGJØRING,
        INFOBREV,
        NESTESAK,
        ANNET
    }
}
