package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandling.kabal.SendTilKabalTask;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AnkeVurderingResultatAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AnkevurderingOppdaterer implements AksjonspunktOppdaterer<AnkeVurderingResultatAksjonspunktDto> {

    private static final Logger LOG = LoggerFactory.getLogger(AnkevurderingOppdaterer.class);
    private static final boolean ER_PROD = Environment.current().isProd();

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private AnkeVurderingTjeneste ankeVurderingTjeneste;
    private AnkeRepository ankeRepository;
    private KlageRepository klageRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    AnkevurderingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AnkevurderingOppdaterer(HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                   AnkeVurderingTjeneste ankeVurderingTjeneste,
                                   AnkeRepository ankeRepository,
                                   KlageRepository klageRepository,
                                   BehandlingRepository behandlingRepository,
                                   ProsessTaskTjeneste prosessTaskTjeneste,
                                   BehandlingVedtakRepository behandlingVedtakRepository) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
        this.ankeRepository = ankeRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.klageRepository = klageRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AnkeVurderingResultatAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var ankeBehandling = param.getBehandling();

        if (!ER_PROD && dto.getSendTilKabal()) {
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
            ankeRepository.settPåAnketKlageBehandling(ankeBehandling.getId(), påAnketKlageBehandlingId);
            var tilKabalTask = ProsessTaskData.forProsessTask(SendTilKabalTask.class);
            tilKabalTask.setBehandling(ankeBehandling.getFagsakId(), ankeBehandling.getId(), ankeBehandling.getAktørId().getId());
            tilKabalTask.setCallIdFraEksisterende();
            tilKabalTask.setProperty(SendTilKabalTask.HJEMMEL_KEY, klageHjemmel.getKode());
            prosessTaskTjeneste.lagre(tilKabalTask);
            var frist = LocalDateTime.now().plusYears(5);
            var apVent = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE, Venteårsak.VENT_KABAL, frist);
            return OppdateringResultat.utenTransisjon().medEkstraAksjonspunktResultat(apVent, AksjonspunktStatus.OPPRETTET).build();
        }

        var ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(ankeBehandling.getId());
        var ankeVurderingResultatHaddeVerdiFørHåndtering = ankeVurderingResultat.isPresent();

        var builder = mapDto(dto, ankeBehandling);
        var påAnketKlageBehandlingId = mapPåAnketKlageBehandlingUuid(dto).orElse(null);
        ankeVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(ankeBehandling, builder, påAnketKlageBehandlingId);

        opprettHistorikkinnslag(ankeBehandling, dto, ankeVurderingResultatHaddeVerdiFørHåndtering, påAnketKlageBehandlingId);

        return OppdateringResultat.utenOveropp();
    }

    private AnkeVurderingResultatEntitet.Builder mapDto(AnkeVurderingResultatAksjonspunktDto apDto,
                                                        Behandling behandling) {
        if (apDto.getAnkeVurdering() == null || AnkeVurdering.UDEFINERT.equals(apDto.getAnkeVurdering())) {
            throw new IllegalArgumentException("Må sette resultat på anke når aksjonspunktet skal løses");
        }
        var builder = ankeVurderingTjeneste.hentAnkeVurderingResultatBuilder(behandling);
        resetVurderingsSpesifikkeValg(builder);
        if (AnkeVurdering.ANKE_AVVIS.equals(apDto.getAnkeVurdering())) {
            builder.medErSubsidiartRealitetsbehandles(apDto.erSubsidiartRealitetsbehandles())
                .medErAnkerIkkePart(apDto.erAnkerIkkePart())
                .medErFristIkkeOverholdt(apDto.erFristIkkeOverholdt())
                .medErIkkeKonkret(apDto.erIkkeKonkret())
                .medErIkkeSignert(apDto.erIkkeSignert());
        } else if (Set.of(AnkeVurdering.ANKE_OMGJOER, AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE,
            AnkeVurdering.ANKE_HJEMSEND_UTEN_OPPHEV).contains(apDto.getAnkeVurdering())) {
            builder.medAnkeOmgjørÅrsak(apDto.getAnkeOmgjoerArsak())
                .medAnkeVurderingOmgjør(apDto.getAnkeVurderingOmgjoer());
        }
        return builder.medAnkeVurdering(apDto.getAnkeVurdering())
            .medBegrunnelse(apDto.getBegrunnelse())
            .medFritekstTilBrev(apDto.getFritekstTilBrev())
            .medGjelderVedtak(apDto.getPåAnketKlageBehandlingUuid() != null);
    }

    private void resetVurderingsSpesifikkeValg(AnkeVurderingResultatEntitet.Builder builder) {
        builder.medAnkeOmgjørÅrsak(AnkeOmgjørÅrsak.UDEFINERT)
            .medAnkeVurderingOmgjør(AnkeVurderingOmgjør.UDEFINERT)
            .medErSubsidiartRealitetsbehandles(false)
            .medErAnkerIkkePart(false)
            .medErFristIkkeOverholdt(false)
            .medErIkkeKonkret(false)
            .medErIkkeSignert(false);
    }

    private void opprettHistorikkinnslag(Behandling behandling,
                                         AnkeVurderingResultatAksjonspunktDto dto,
                                         boolean endreAnke,
                                         Long påAnketKlageBehandlingId) {
        var ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
        var ankeResultat = ankeRepository.hentEllerOpprettAnkeResultat(behandling.getId());
        var ankeVurdering = AnkeVurdering.fraKode(dto.getAnkeVurdering().getKode());
        var ankeVurderingOmgjør = dto.getAnkeVurderingOmgjoer() != null ? AnkeVurderingOmgjør.fraKode(
            dto.getAnkeVurderingOmgjoer().getKode()) : null;
        var omgjørÅrsak = dto.getAnkeOmgjoerArsak() != null ? dto.getAnkeOmgjoerArsak() : null;

        var resultat = AnkeVurderingTjeneste.konverterAnkeVurderingTilResultatType(ankeVurdering, ankeVurderingOmgjør);
        var historiebygger = new HistorikkInnslagTekstBuilder();

        if (!endreAnke) {
            historiebygger.medEndretFelt(HistorikkEndretFeltType.ANKE_RESULTAT, null,
                resultat != null ? resultat.getNavn() : null);
            if (dto.getAnkeOmgjoerArsak() != null && omgjørÅrsak != null) {
                historiebygger.medEndretFelt(HistorikkEndretFeltType.ANKE_OMGJØR_ÅRSAK, null, omgjørÅrsak.getNavn());
            } else if (dto.erAnkerIkkePart() || dto.erFristIkkeOverholdt() || dto.erIkkeKonkret()
                || dto.erIkkeSignert()) {
                historiebygger
                    .medEndretFelt(HistorikkEndretFeltType.ER_ANKER_IKKE_PART, null, dto.erAnkerIkkePart())
                    .medEndretFelt(HistorikkEndretFeltType.ER_ANKEFRIST_IKKE_OVERHOLDT, null, dto.erFristIkkeOverholdt())
                    .medEndretFelt(HistorikkEndretFeltType.ER_ANKE_IKKE_KONKRET, null, dto.erIkkeKonkret())
                    .medEndretFelt(HistorikkEndretFeltType.ER_ANKEN_IKKE_SIGNERT, null, dto.erIkkeSignert());
            }
        } else {
            finnOgSettOppEndredeHistorikkFelter(ankeVurderingResultat.get(), historiebygger, dto, ankeResultat,
                omgjørÅrsak, resultat, påAnketKlageBehandlingId);
        }
        historiebygger.medBegrunnelse(dto.getBegrunnelse());
        historiebygger.medSkjermlenke(SkjermlenkeType.ANKE_VURDERING);

        var innslag = new Historikkinnslag();
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setType(HistorikkinnslagType.ANKE_BEH);
        innslag.setBehandlingId(behandling.getId());
        historiebygger.build(innslag);

        historikkTjenesteAdapter.lagInnslag(innslag);
    }

    private void finnOgSettOppEndredeHistorikkFelter(AnkeVurderingResultatEntitet ankeVurderingResultat,
                                                     HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder,
                                                     AnkeVurderingResultatAksjonspunktDto dto,
                                                     AnkeResultatEntitet ankeResultat,
                                                     Kodeverdi årsakFraDto,
                                                     HistorikkResultatType resultat,
                                                     Long påAnketKlageBehandlingId) {
        if (erVedtakOppdatert(ankeResultat, påAnketKlageBehandlingId)) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_ANKET_BEHANDLINGID,
                hentPåAnketKlageBehandlingTekst(ankeResultat.getPåAnketKlageBehandlingId().orElse(null)),
                hentPåAnketKlageBehandlingTekst(påAnketKlageBehandlingId));
        }
        if (erAnkeVurderingEndret(ankeVurderingResultat, dto)) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ANKE_RESULTAT,
                ankeVurderingResultat.getAnkeVurdering().getNavn(), resultat.getNavn());
        }
        if (erAnkeOmgjørÅrsakEndret(ankeVurderingResultat, dto, årsakFraDto)) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ANKE_OMGJØR_ÅRSAK,
                ankeVurderingResultat.getAnkeOmgjørÅrsak().getNavn(), dto.getAnkeOmgjoerArsak().getNavn());
        }
        if (ankeVurderingResultat.erAnkerIkkePart() != dto.erAnkerIkkePart()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_ANKER_IKKE_PART,
                ankeVurderingResultat.erAnkerIkkePart(), dto.erAnkerIkkePart());
        }
        if (ankeVurderingResultat.erFristIkkeOverholdt() != dto.erFristIkkeOverholdt()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_ANKEFRIST_IKKE_OVERHOLDT,
                ankeVurderingResultat.erAnkerIkkePart(), dto.erFristIkkeOverholdt());
        }
        if (ankeVurderingResultat.erIkkeKonkret() != dto.erIkkeKonkret()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_ANKE_IKKE_KONKRET,
                ankeVurderingResultat.erIkkeKonkret(), dto.erIkkeKonkret());
        }
        if (ankeVurderingResultat.erIkkeSignert() != dto.erIkkeSignert()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_ANKEN_IKKE_SIGNERT,
                ankeVurderingResultat.erIkkeSignert(), dto.erIkkeSignert());
        }
    }

    private Optional<Long> mapPåAnketKlageBehandlingUuid(AnkeVurderingResultatAksjonspunktDto dto) {
        var påAnketKlageBehandlingUuid = dto.getPåAnketKlageBehandlingUuid();
        if (påAnketKlageBehandlingUuid != null) {
            return Optional.of(behandlingRepository.hentBehandling(påAnketKlageBehandlingUuid).getId());
        }
        return Optional.empty();
    }

    private String hentPåAnketKlageBehandlingTekst(Long behandlingId) {
        if (behandlingId == null) {
            return "Ikke påanket et vedtak";
        }
        var påAnketKlageBehandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtaksDatoPåAnketKlageBehandling = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId)
            .map(it -> it.getVedtaksdato());
        return påAnketKlageBehandling.getType().getNavn() + " " + vedtaksDatoPåAnketKlageBehandling.map(
            dato -> dato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).orElse("");
    }

    private boolean erVedtakOppdatert(AnkeResultatEntitet ankeResultat, Long nyPåAnketKlageBehandling) {
        var harLagretPåAnketKlageBehandling = ankeResultat.getPåAnketKlageBehandlingId().isPresent();

        if (!harLagretPåAnketKlageBehandling && nyPåAnketKlageBehandling == null) {
            return false;
        }
        if (!harLagretPåAnketKlageBehandling || nyPåAnketKlageBehandling == null) {
            return true;
        }
        return !ankeResultat.getPåAnketKlageBehandlingId().get().equals(nyPåAnketKlageBehandling);
    }

    private boolean erAnkeVurderingEndret(AnkeVurderingResultatEntitet ankeVurderingResultat,
                                          AnkeVurderingResultatAksjonspunktDto dto) {
        return ankeVurderingResultat.getAnkeVurdering() != null && !ankeVurderingResultat.getAnkeVurdering()
            .equals(dto.getAnkeVurdering());
    }

    private boolean erAnkeOmgjørÅrsakEndret(AnkeVurderingResultatEntitet ankeVurderingResultat,
                                            AnkeVurderingResultatAksjonspunktDto dto,
                                            Kodeverdi årsakFraDto) {
        return ankeVurderingResultat.getAnkeOmgjørÅrsak() != null && dto.getAnkeOmgjoerArsak() != null
            && årsakFraDto != null && !ankeVurderingResultat.getAnkeOmgjørÅrsak().equals(dto.getAnkeOmgjoerArsak());
    }
}
