package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.klage.KlageFormkravAdapter;
import no.nav.foreldrepenger.behandling.klage.KlageFormkravTjeneste;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingAdapter;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KlageFormkravAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class KlageFormkravOppdaterer implements AksjonspunktOppdaterer<KlageFormkravAksjonspunktDto> {

    private KlageFormkravTjeneste klageFormkravTjeneste;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private KlageRepository klageRepository;
    private AksjonspunktRepository aksjonspunktRepository = new AksjonspunktRepository();
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    KlageFormkravOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KlageFormkravOppdaterer(KlageFormkravTjeneste klageFormkravTjeneste, KlageVurderingTjeneste klageVurderingTjeneste,
                                   HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   KlageRepository klageRepository,
                                   BehandlingVedtakRepository behandlingVedtakRepository) {
        this.klageRepository = klageRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.klageFormkravTjeneste = klageFormkravTjeneste;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(KlageFormkravAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        String aksjonspunktKode = dto.getKode();
        Behandling klageBehandling = param.getBehandling();
        KlageResultatEntitet klageResultat = klageRepository.hentKlageResultat(klageBehandling);
        KlageVurdertAv klageVurdertAv = getKlageVurdertAv(aksjonspunktKode);
        Optional<KlageFormkravEntitet> klageFormkrav = klageRepository.hentKlageFormkrav(klageBehandling, klageVurdertAv);
        AksjonspunktDefinisjon apDefFormkrav = AksjonspunktDefinisjon.fraKode(aksjonspunktKode);
        var skjermlenkeType = getSkjermlenkeType(aksjonspunktKode);

        opprettHistorikkinnslag(klageBehandling, apDefFormkrav, dto, klageFormkrav, klageResultat, skjermlenkeType);
        Optional<KlageAvvistÅrsak> optionalAvvistÅrsak = vurderOgLagreFormkrav(dto, klageBehandling, klageResultat);
        if (optionalAvvistÅrsak.isPresent()) {
            lagreKlageVurderingResultatMedAvvistKlage(klageBehandling, dto);
            return OppdateringResultat.medFremoverHoppTotrinn(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_VEDTAK);
        }
        klageBehandling.getAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(apDefFormkrav))
            .findFirst()
            .ifPresent(ap -> aksjonspunktRepository.fjernToTrinnsBehandlingKreves(ap));

        return OppdateringResultat.utenTransisjon().build();
    }

    private Optional<KlageAvvistÅrsak> vurderOgLagreFormkrav(KlageFormkravAksjonspunktDto dto, Behandling behandling, KlageResultatEntitet klageResultat) {
        KlageVurdertAv klageVurdertAv = getKlageVurdertAv(dto.getKode());
        boolean gjelderVedtak = dto.hentpåKlagdBehandlingId() != null;
        if (gjelderVedtak || dto.hentpåKlagdBehandlingId() == null && klageResultat.getPåKlagdBehandling().isPresent()) {
            klageFormkravTjeneste.oppdaterKlageMedPåklagetBehandling(behandling.getId(), dto.hentpåKlagdBehandlingId());
        }
        KlageFormkravAdapter adapter = new KlageFormkravAdapter();
        adapter.setErKlagerPart(dto.erKlagerPart());
        adapter.setErFristOverholdt(dto.erFristOverholdt());
        adapter.setErKonkret(dto.erKonkret());
        adapter.setErSignert(dto.erSignert());
        adapter.setGjelderVedtak(gjelderVedtak);
        adapter.setBegrunnelse(dto.getBegrunnelse());
        adapter.setKlageBehandlingId(behandling.getId());
        adapter.setKlageVurdertAv(klageVurdertAv);

        klageFormkravTjeneste.lagreFormkrav(adapter);
        return utledAvvistÅrsak(dto, gjelderVedtak);

    }

    private Optional<KlageAvvistÅrsak> utledAvvistÅrsak(KlageFormkravAksjonspunktDto dto, boolean gjelderVedtak) {
        if (!gjelderVedtak) {
            return Optional.of(KlageAvvistÅrsak.IKKE_PAKLAGD_VEDTAK);
        }
        if (!dto.erKlagerPart()) {
            return Optional.of(KlageAvvistÅrsak.KLAGER_IKKE_PART);
        }
        if (!dto.erFristOverholdt()) {
            return Optional.of(KlageAvvistÅrsak.KLAGET_FOR_SENT);
        }
        if (!dto.erKonkret()) {
            return Optional.of(KlageAvvistÅrsak.IKKE_KONKRET);
        }
        if (!dto.erSignert()) {
            return Optional.of(KlageAvvistÅrsak.IKKE_SIGNERT);
        }
        return Optional.empty();
    }

    private void lagreKlageVurderingResultatMedAvvistKlage(Behandling klageBehandling, KlageFormkravAksjonspunktDto dto) {
        boolean erNfpAksjonspunkt = AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP.getKode().equals(dto.getKode());
        final KlageVurderingAdapter vurderingDto = new KlageVurderingAdapter(KlageVurdering.AVVIS_KLAGE.getKode(), null,
            null, erNfpAksjonspunkt, null, null, false);
        klageVurderingTjeneste.oppdater(klageBehandling, vurderingDto);
    }

    private void opprettHistorikkinnslag(Behandling klageBehandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                         KlageFormkravAksjonspunktDto formkravDto, Optional<KlageFormkravEntitet> klageFormkrav,
                                         KlageResultatEntitet klageResultat, SkjermlenkeType skjermlenkeType) {
        HistorikkinnslagType historikkinnslagType = erNfpAksjonspunt(aksjonspunktDefinisjon) ? HistorikkinnslagType.KLAGE_BEH_NFP
            : HistorikkinnslagType.KLAGE_BEH_NK;
        HistorikkInnslagTekstBuilder historiebygger = new HistorikkInnslagTekstBuilder()
            .medSkjermlenke(skjermlenkeType)
            .medBegrunnelse(formkravDto.getBegrunnelse());
        if (!klageFormkrav.isPresent()) {
            settOppHistorikkFelter(historiebygger, formkravDto);
        } else {
            finnOgSettOppEndredeHistorikkFelter(klageFormkrav.get(), historiebygger, formkravDto, klageResultat);
        }
        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setType(historikkinnslagType);
        innslag.setBehandlingId(klageBehandling.getId());
        historiebygger.build(innslag);
        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

    private String hentPåklagdBehandlingTekst(Long behandlingId) {
        if (behandlingId == null) {
            return "Ikke påklagd et vedtak";
        }
        Behandling påKlagdBehandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<LocalDate> vedtaksDatoPåklagdBehandling = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandlingId)
            .map(it -> it.getVedtaksdato());
        return påKlagdBehandling.getType().getNavn() + " " +
            vedtaksDatoPåklagdBehandling
                .map(dato -> dato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .orElse("");
    }

    private void settOppHistorikkFelter(HistorikkInnslagTekstBuilder historiebygger, KlageFormkravAksjonspunktDto formkravDto) {
        var påKlagdBehandling = hentPåklagdBehandlingTekst(formkravDto.hentpåKlagdBehandlingId());
        historiebygger
            .medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID, null, påKlagdBehandling)
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGER_PART, null, formkravDto.erKlagerPart())
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGEFRIST_OVERHOLDT, null, formkravDto.erFristOverholdt())
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGEN_SIGNERT, null, formkravDto.erSignert())
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGE_KONKRET, null, formkravDto.erKonkret());

    }

    private void finnOgSettOppEndredeHistorikkFelter(KlageFormkravEntitet klageFormkrav, HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder,
                                                     KlageFormkravAksjonspunktDto formkravDto, KlageResultatEntitet klageResultat) {
        if (erVedtakOppdatert(klageResultat, formkravDto)) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåklagdBehandlingTekst(klageResultat.getPåKlagdBehandling().map(Behandling::getId).orElse(null)),
                hentPåklagdBehandlingTekst(formkravDto.hentpåKlagdBehandlingId()));
        }
        if (klageFormkrav.erKlagerPart() != formkravDto.erKlagerPart()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGER_PART, klageFormkrav.erKlagerPart(), formkravDto.erKlagerPart());
        }
        if (klageFormkrav.erFristOverholdt() != formkravDto.erFristOverholdt()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGEFRIST_OVERHOLDT, klageFormkrav.erFristOverholdt(),
                formkravDto.erFristOverholdt());
        }
        if (klageFormkrav.erSignert() != formkravDto.erSignert()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGEN_SIGNERT, klageFormkrav.erSignert(), formkravDto.erSignert());
        }
        if (klageFormkrav.erKonkret() != formkravDto.erKonkret()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGE_KONKRET, klageFormkrav.erKonkret(), formkravDto.erKonkret());
        }
    }

    private KlageVurdertAv getKlageVurdertAv(String apKode) {
        return apKode.equals(VURDERING_AV_FORMKRAV_KLAGE_NFP.getKode()) ? KlageVurdertAv.NFP : KlageVurdertAv.NK;
    }

    private SkjermlenkeType getSkjermlenkeType(String apKode) {
        return apKode.equals(VURDERING_AV_FORMKRAV_KLAGE_NFP.getKode()) ? SkjermlenkeType.FORMKRAV_KLAGE_NFP : SkjermlenkeType.FORMKRAV_KLAGE_KA;
    }

    private boolean erVedtakOppdatert(KlageResultatEntitet klageResultat, KlageFormkravAksjonspunktDto formkravDto) {
        Long nyPåklagdBehandling = formkravDto.hentpåKlagdBehandlingId();
        boolean harLagretPåklagdBehandling = klageResultat.getPåKlagdBehandling().isPresent();

        if (!harLagretPåklagdBehandling && nyPåklagdBehandling == null) {
            return false;
        }
        if (!harLagretPåklagdBehandling && nyPåklagdBehandling != null) {
            return true;
        }
        if (harLagretPåklagdBehandling && nyPåklagdBehandling == null) {
            return true;
        }
        return !klageResultat.getPåKlagdBehandling().get().getId().equals(nyPåklagdBehandling);
    }

    private boolean erNfpAksjonspunt(AksjonspunktDefinisjon apDef) {
        return Objects.equals(KlageVurdertAv.NFP, getKlageVurdertAv(apDef.getKode()));
    }
}
