package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import java.time.format.DateTimeFormatter;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.BasisKodeverdi;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AnkeVurderingResultatAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AnkevurderingOppdaterer implements AksjonspunktOppdaterer<AnkeVurderingResultatAksjonspunktDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private AnkeVurderingTjeneste ankeVurderingTjeneste;
    private AnkeRepository ankeRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    AnkevurderingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AnkevurderingOppdaterer(HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                   AnkeVurderingTjeneste ankeVurderingTjeneste,
                                   AnkeRepository ankeRepository,
                                   BehandlingRepository behandlingRepository,
                                   BehandlingVedtakRepository behandlingVedtakRepository) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
        this.ankeRepository = ankeRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    @Override
    public OppdateringResultat oppdater(AnkeVurderingResultatAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();

        var ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
        var ankeVurderingResultatHaddeVerdiFørHåndtering = ankeVurderingResultat.isPresent();

        var builder = mapDto(dto, behandling);
        var påanketBehandlingId = dto.getPåAnketBehandlingId();
        if (påanketBehandlingId == null && dto.getVedtakBehandlingUuid() != null) {
            påanketBehandlingId = behandlingRepository.hentBehandling(dto.getVedtakBehandlingUuid()).getId();
        }
        ankeVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(behandling, builder, påanketBehandlingId);

        opprettHistorikkinnslag(behandling, dto, ankeVurderingResultatHaddeVerdiFørHåndtering);

        return OppdateringResultat.utenOveropp();
    }

    private AnkeVurderingResultatEntitet.Builder mapDto(AnkeVurderingResultatAksjonspunktDto apDto,
                                                        Behandling behandling) {
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
            .medGjelderVedtak(apDto.getPåAnketBehandlingId() != null || apDto.getVedtakBehandlingUuid() != null);
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
                                         boolean endreAnke) {
        var ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
        var ankeResultat = ankeRepository.hentEllerOpprettAnkeResultat(behandling.getId());
        var ankeVurdering = AnkeVurdering.fraKode(dto.getAnkeVurdering().getKode());
        var ankeVurderingOmgjør = dto.getAnkeVurderingOmgjoer() != null ? AnkeVurderingOmgjør.fraKode(
            dto.getAnkeVurderingOmgjoer().getKode()) : null;
        var omgjørÅrsak = dto.getAnkeOmgjoerArsak() != null ? dto.getAnkeOmgjoerArsak() : null;

        var resultat = konverterAnkeVurderingTilResultatType(ankeVurdering, ankeVurderingOmgjør);
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
                omgjørÅrsak, resultat);
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

    private HistorikkResultatType konverterAnkeVurderingTilResultatType(AnkeVurdering vurdering,
                                                                        AnkeVurderingOmgjør ankeVurderingOmgjør) {
        if (AnkeVurdering.ANKE_AVVIS.equals(vurdering)) {
            return HistorikkResultatType.ANKE_AVVIS;
        }
        if (AnkeVurdering.ANKE_OMGJOER.equals(vurdering)) {
            if (AnkeVurderingOmgjør.ANKE_DELVIS_OMGJOERING_TIL_GUNST.equals(ankeVurderingOmgjør)) {
                return HistorikkResultatType.ANKE_DELVIS_OMGJOERING_TIL_GUNST;
            }
            if (AnkeVurderingOmgjør.ANKE_TIL_UGUNST.equals(ankeVurderingOmgjør)) {
                return HistorikkResultatType.ANKE_TIL_UGUNST;
            }
            if (AnkeVurderingOmgjør.ANKE_TIL_GUNST.equals(ankeVurderingOmgjør)) {
                return HistorikkResultatType.ANKE_TIL_GUNST;
            }
            return HistorikkResultatType.ANKE_OMGJOER;
        }
        if (AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE.equals(vurdering)) {
            return HistorikkResultatType.ANKE_OPPHEVE_OG_HJEMSENDE;
        }
        if (AnkeVurdering.ANKE_HJEMSEND_UTEN_OPPHEV.equals(vurdering)) {
            return HistorikkResultatType.ANKE_HJEMSENDE;
        }
        if (AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK.equals(vurdering)) {
            return HistorikkResultatType.ANKE_STADFESTET_VEDTAK;
        }
        return null;
    }

    private void finnOgSettOppEndredeHistorikkFelter(AnkeVurderingResultatEntitet ankeVurderingResultat,
                                                     HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder,
                                                     AnkeVurderingResultatAksjonspunktDto dto,
                                                     AnkeResultatEntitet ankeResultat,
                                                     BasisKodeverdi årsakFraDto,
                                                     HistorikkResultatType resultat) {
        if (erVedtakOppdatert(ankeResultat, dto)) {
            final Long påAnketBehandlingId;
            if (dto.getPåAnketBehandlingId() == null && dto.getVedtakBehandlingUuid() == null) {
                påAnketBehandlingId = null;
            } else if (dto.getPåAnketBehandlingId() == null) {
                påAnketBehandlingId = behandlingRepository.hentBehandling(dto.getVedtakBehandlingUuid()).getId();
            } else {
                påAnketBehandlingId = dto.getPåAnketBehandlingId();
            }
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_ANKET_BEHANDLINGID,
                hentPåanketBehandlingTekst(ankeResultat.getPåAnketBehandlingId().orElse(null)),
                hentPåanketBehandlingTekst(påAnketBehandlingId));
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

    private String hentPåanketBehandlingTekst(Long behandlingId) {
        if (behandlingId == null) {
            return "Ikke påanket et vedtak";
        }
        var påAnketBehandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtaksDatoPåanketBehandling = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId)
            .map(it -> it.getVedtaksdato());
        return påAnketBehandling.getType().getNavn() + " " + vedtaksDatoPåanketBehandling.map(
            dato -> dato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).orElse("");
    }

    private boolean erVedtakOppdatert(AnkeResultatEntitet ankeResultat, AnkeVurderingResultatAksjonspunktDto dto) {
        var nyPåAnketBehandling = dto.getPåAnketBehandlingId() == null ? behandlingRepository.hentBehandling(dto.getVedtakBehandlingUuid())
            : dto.getPåAnketBehandlingId();
        var harLagretPåAnketBehandling = ankeResultat.getPåAnketBehandlingId().isPresent();

        if (!harLagretPåAnketBehandling && nyPåAnketBehandling == null) {
            return false;
        }
        if (!harLagretPåAnketBehandling || nyPåAnketBehandling == null) {
            return true;
        }
        return !ankeResultat.getPåAnketBehandlingId().get().equals(nyPåAnketBehandling);
    }

    private boolean erAnkeVurderingEndret(AnkeVurderingResultatEntitet ankeVurderingResultat,
                                          AnkeVurderingResultatAksjonspunktDto dto) {
        return ankeVurderingResultat.getAnkeVurdering() != null && !ankeVurderingResultat.getAnkeVurdering()
            .equals(dto.getAnkeVurdering());
    }

    private boolean erAnkeOmgjørÅrsakEndret(AnkeVurderingResultatEntitet ankeVurderingResultat,
                                            AnkeVurderingResultatAksjonspunktDto dto,
                                            BasisKodeverdi årsakFraDto) {
        return ankeVurderingResultat.getAnkeOmgjørÅrsak() != null && dto.getAnkeOmgjoerArsak() != null
            && årsakFraDto != null && !ankeVurderingResultat.getAnkeOmgjørÅrsak().equals(dto.getAnkeOmgjoerArsak());
    }
}
