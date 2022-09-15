package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandling.kabal.SendTilKabalTRTask;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AnkeMerknaderResultatAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AnkeMerknaderOppdaterer implements AksjonspunktOppdaterer<AnkeMerknaderResultatAksjonspunktDto> {

    private static final AksjonspunktDefinisjon VENT_TRYGDERETT = AksjonspunktDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN;

    private static final boolean ER_PROD = Environment.current().isProd();

    private BehandlingRepository behandlingRepository;
    private AnkeVurderingTjeneste ankeVurderingTjeneste;
    private AnkeRepository ankeRepository;
    private KlageRepository klageRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    AnkeMerknaderOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AnkeMerknaderOppdaterer(BehandlingRepository behandlingRepository,
                                   AnkeRepository ankeRepository,
                                   KlageRepository klageRepository,
                                   ProsessTaskTjeneste taskTjeneste,
                                   AnkeVurderingTjeneste ankeVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ankeRepository = ankeRepository;
        this.klageRepository = klageRepository;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
        this.prosessTaskTjeneste = taskTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AnkeMerknaderResultatAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var ankeBehandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var utvalgteSBH = Optional.ofNullable(SubjectHandler.getSubjectHandler().getUid())
            .filter(u -> Set.of("A100182", "E137084").contains(u))
            .isPresent();

        if ((!ER_PROD || utvalgteSBH) && Optional.ofNullable(dto.getSendTilKabal()).orElse(false)) {
            var ankeresultat = ankeRepository.hentAnkeResultat(ankeBehandling.getId());
            var påAnketKlageBehandlingId = mapPåAnketKlageBehandlingUuid(dto)
                .or(() ->  ankeresultat.flatMap(AnkeResultatEntitet::getPåAnketKlageBehandlingId))
                .orElseThrow(() -> new FunksjonellException("FP-ANKEKLAGE", "Mangler påanket klage", "Velg klagebehandling"))
                ;
            var hjemmelFraKlage = klageRepository.hentKlageVurderingResultat(påAnketKlageBehandlingId, KlageVurdertAv.NFP)
                .map(KlageVurderingResultat::getKlageHjemmel);
            var klageHjemmel = Optional.ofNullable(dto.getKlageHjemmel())
                .filter(h -> !KlageHjemmel.UDEFINERT.equals(h))
                .or(() -> hjemmelFraKlage)
                .orElseGet(() -> KlageHjemmel.standardHjemmelForYtelse(ankeBehandling.getFagsakYtelseType()));
            if (KlageHjemmel.UDEFINERT.equals(klageHjemmel)) {
                throw new FunksjonellException("FP-HJEMMEL", "Mangler hjemmel", "Velg hjemmel");
            }
            var sendtTryderettenDato = dto.getSendtTilTrygderetten();
            if (sendtTryderettenDato == null) {
                throw new FunksjonellException("FP-OVERSENDT", "Mangler dato anken ble sendt Trygderetten", "Velg dato");
            }
            ankeRepository.settPåAnketKlageBehandling(ankeBehandling.getId(), påAnketKlageBehandlingId);
            ankeRepository.settAnkeSendtTrygderettenDato(ankeBehandling.getId(), sendtTryderettenDato);
            var tilKabalTask = ProsessTaskData.forProsessTask(SendTilKabalTRTask.class);
            tilKabalTask.setBehandling(ankeBehandling.getFagsakId(), ankeBehandling.getId(), ankeBehandling.getAktørId().getId());
            tilKabalTask.setCallIdFraEksisterende();
            tilKabalTask.setProperty(SendTilKabalTRTask.HJEMMEL_KEY, klageHjemmel.getKode());
            prosessTaskTjeneste.lagre(tilKabalTask);
            var frist = LocalDateTime.now().plusYears(5);
            var apVent = AksjonspunktResultat.opprettForAksjonspunktMedFrist(VENT_TRYGDERETT, Venteårsak.VENT_KABAL, frist);
            return OppdateringResultat.utenTransisjon().medEkstraAksjonspunktResultat(apVent, AksjonspunktStatus.OPPRETTET).build();
        }


        håndterAnkeVurdering(ankeBehandling, dto);

        var builder = OppdateringResultat.utenTransisjon();
        if (!dto.skalAvslutteBehandling()) {
            var frist = ankeBehandling.getAksjonspunktMedDefinisjonOptional(VENT_TRYGDERETT).map(Aksjonspunkt::getFristTid)
                .orElseGet(() -> LocalDateTime.now().plus(Objects.requireNonNull(VENT_TRYGDERETT.getFristPeriod())));
            if (frist.isBefore(LocalDateTime.now().plusWeeks(4)))
                frist = LocalDateTime.now().plusWeeks(4);
            var apVent = AksjonspunktResultat.opprettForAksjonspunktMedFrist(VENT_TRYGDERETT, Venteårsak.ANKE_OVERSENDT_TIL_TRYGDERETTEN, frist);
            builder.medEkstraAksjonspunktResultat(apVent, AksjonspunktStatus.OPPRETTET);
        }
        return builder.build();
    }

    private void håndterAnkeVurdering(Behandling behandling, AnkeMerknaderResultatAksjonspunktDto dto) {
        ankeVurderingTjeneste.oppdaterBekreftetMerknaderAksjonspunkt(behandling, dto.erMerknaderMottatt(), dto.getMerknadKommentar(),
            dto.getTrygderettVurdering(), dto.getTrygderettVurderingOmgjoer(), dto.getTrygderettOmgjoerArsak());
    }

    private Optional<Long> mapPåAnketKlageBehandlingUuid(AnkeMerknaderResultatAksjonspunktDto dto) {
        return Optional.ofNullable(dto.getPåAnketKlageBehandlingUuid())
            .map(kb -> behandlingRepository.hentBehandling(kb).getId());
    }

}
