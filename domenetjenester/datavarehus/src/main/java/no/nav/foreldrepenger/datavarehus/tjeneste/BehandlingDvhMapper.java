package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingDvh;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

public class BehandlingDvhMapper {

    private BehandlingDvhMapper() {
    }

    private static final ArrayList<BehandlingResultatType> AVBRUTT_BEHANDLINGSRESULTAT = new ArrayList<>();

    static {
        AVBRUTT_BEHANDLINGSRESULTAT.add(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET);
        AVBRUTT_BEHANDLINGSRESULTAT.add(BehandlingResultatType.HENLAGT_FEILOPPRETTET);
        AVBRUTT_BEHANDLINGSRESULTAT.add(BehandlingResultatType.HENLAGT_BRUKER_DØD);
        AVBRUTT_BEHANDLINGSRESULTAT.add(BehandlingResultatType.MERGET_OG_HENLAGT);
        AVBRUTT_BEHANDLINGSRESULTAT.add(BehandlingResultatType.HENLAGT_SØKNAD_MANGLER);
    }

    public static BehandlingDvh map(Behandling behandling, // NOSONAR
                                    Behandlingsresultat behandlingsresultat,
                                    LocalDateTime mottattTidspunkt,
                                    Optional<BehandlingVedtak> vedtak,
                                    Optional<FamilieHendelseGrunnlagEntitet> fh,
                                    Optional<KlageResultatEntitet> klageResultat,
                                    Optional<AnkeResultatEntitet> ankeResultat,
                                    Optional<ForeldrepengerUttak> uttak,
                                    Optional<LocalDate> skjæringstidspunkt,
                                    FagsakMarkering fagsakMarkering) {

        return BehandlingDvh.builder()
            .ansvarligBeslutter(behandling.getAnsvarligBeslutter())
            .ansvarligSaksbehandler(behandling.getAnsvarligSaksbehandler())
            .behandlendeEnhet(behandling.getBehandlendeEnhet())
            .behandlingId(behandling.getId())
            .behandlingUuid(behandling.getUuid())
            .behandlingResultatType(behandlingsresultat == null ? null : behandlingsresultat.getBehandlingResultatType().getKode())
            .behandlingStatus(behandling.getStatus().getKode())
            .behandlingType(behandling.getType().getKode())
            .endretAv(CommonDvhMapper.finnEndretAvEllerOpprettetAv(behandling))
            .fagsakId(behandling.getFagsakId())
            .funksjonellTid(LocalDateTime.now())
            .opprettetDato(behandling.getOpprettetDato().toLocalDate())
            .utlandstilsnitt(getUtlandstilsnitt(fagsakMarkering))
            .toTrinnsBehandling(behandling.isToTrinnsBehandling())
            .vedtakId(vedtak.map(BehandlingVedtak::getId).orElse(null))
            .relatertBehandling(getRelatertBehandling(behandling, klageResultat, ankeResultat))
            .ferdig(mapFerdig(behandling))
            .vedtatt(behandlingsresultat != null && mapVedtatt(behandlingsresultat, behandling.getFagsak().getStatus()))
            .avbrutt(behandlingsresultat != null && mapAvbrutt(behandlingsresultat, behandling.getFagsak().getStatus()))
            .soeknadFamilieHendelse(mapSoeknadFamilieHendelse(fh))
            .bekreftetFamilieHendelse(mapbekreftetFamilieHendelse(fh))
            .overstyrtFamilieHendelse(mapoverstyrtFamilieHendelse(fh))
            .medMottattTidspunkt(mottattTidspunkt)
            .medFoersteStoenadsdag(CommonDvhMapper.foersteStoenadsdag(uttak, skjæringstidspunkt))
            .build();
    }

    private static String getUtlandstilsnitt(FagsakMarkering fagsakMarkering) {
        return FagsakMarkering.BOSATT_UTLAND.equals(fagsakMarkering) || FagsakMarkering.EØS_BOSATT_NORGE.equals(fagsakMarkering) ?
            fagsakMarkering.name() : FagsakMarkering.NASJONAL.name();
    }

    /**
     * Er det klage, hentes relatert behandling fra klageresultat. Hvis ikke hentes relatert behandling fra orginalbehandling-referansen på behandlingen.
     */
    private static Long getRelatertBehandling(Behandling behandling,
                                              Optional<KlageResultatEntitet> klageResultat,
                                              Optional<AnkeResultatEntitet> ankeResultat) {
        if (BehandlingType.ANKE.equals(behandling.getType()) && ankeResultat.isPresent()) {
            return ankeResultat.flatMap(AnkeResultatEntitet::getPåAnketKlageBehandlingId).orElse(null);
        }
        if (BehandlingType.KLAGE.equals(behandling.getType()) && klageResultat.isPresent()) {
            return klageResultat.flatMap(KlageResultatEntitet::getPåKlagdBehandlingId).orElse(null);
        }
        return behandling.getOriginalBehandlingId().orElse(null);
    }

    private static String mapSoeknadFamilieHendelse(Optional<FamilieHendelseGrunnlagEntitet> fh) {
        return fh.map(f -> f.getSøknadVersjon().getType().getKode()).orElse(null);
    }

    private static String mapbekreftetFamilieHendelse(Optional<FamilieHendelseGrunnlagEntitet> fh) {
        return fh.flatMap(f -> f.getBekreftetVersjon().map(bv -> bv.getType().getKode())).orElse(null);
    }

    private static String mapoverstyrtFamilieHendelse(Optional<FamilieHendelseGrunnlagEntitet> fh) {
        return fh.flatMap(f -> f.getOverstyrtVersjon().map(bv -> bv.getType().getKode())).orElse(null);
    }

    private static boolean mapAvbrutt(Behandlingsresultat behandlingsresultat, FagsakStatus fagsakStatus) {
        if (FagsakStatus.AVSLUTTET.equals(fagsakStatus)) {
            return AVBRUTT_BEHANDLINGSRESULTAT.stream().anyMatch(type -> type.equals(behandlingsresultat.getBehandlingResultatType()));
        }
        return false;
    }

    private static boolean mapVedtatt(Behandlingsresultat behandlingsresultat, FagsakStatus fagsakStatus) {
        var behandlingResultatType = behandlingsresultat.getBehandlingResultatType();
        if (FagsakStatus.AVSLUTTET.equals(fagsakStatus)) {
            return BehandlingResultatType.AVSLÅTT.equals(behandlingResultatType);
        }
        if (FagsakStatus.LØPENDE.equals(fagsakStatus)) {
            return BehandlingResultatType.INNVILGET.equals(behandlingResultatType);
        }
        return false;
    }

    private static boolean mapFerdig(Behandling behandling) {
        return FagsakStatus.AVSLUTTET.equals(behandling.getFagsak().getStatus());
    }

}
