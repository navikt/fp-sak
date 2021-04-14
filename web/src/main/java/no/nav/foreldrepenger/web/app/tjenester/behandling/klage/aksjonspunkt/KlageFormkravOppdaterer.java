package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KlageFormkravAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class KlageFormkravOppdaterer implements AksjonspunktOppdaterer<KlageFormkravAksjonspunktDto> {

    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private AksjonspunktRepository aksjonspunktRepository = new AksjonspunktRepository();
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    private FptilbakeRestKlient fptilbakeRestKlient;

    KlageFormkravOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KlageFormkravOppdaterer(KlageVurderingTjeneste klageVurderingTjeneste,
                                   HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   BehandlingVedtakRepository behandlingVedtakRepository,
                                   FptilbakeRestKlient fptilbakeRestKlient) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
    }

    @Override
    public OppdateringResultat oppdater(KlageFormkravAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var apDefFormkrav = AksjonspunktDefinisjon.fraKode(dto.getKode());
        var klageVurdertAv = getKlageVurdertAv(apDefFormkrav);

        var klageBehandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var klageResultat = klageVurderingTjeneste.hentEvtOpprettKlageResultat(klageBehandling);
        var klageFormkrav = klageVurderingTjeneste.hentKlageFormkrav(klageBehandling, klageVurdertAv);

        opprettHistorikkinnslag(klageBehandling, apDefFormkrav, dto, klageFormkrav, klageResultat);
        var optionalAvvistÅrsak = vurderOgLagreFormkrav(dto, klageBehandling, klageResultat, klageVurdertAv);
        if (optionalAvvistÅrsak.isPresent()) {
            lagreKlageVurderingResultatMedAvvistKlage(klageBehandling, klageVurdertAv);
            return OppdateringResultat.medFremoverHoppTotrinn(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_VEDTAK);
        }
        klageBehandling.getÅpentAksjonspunktMedDefinisjonOptional(apDefFormkrav).ifPresent(ap -> aksjonspunktRepository.fjernToTrinnsBehandlingKreves(ap));

        return OppdateringResultat.utenTransisjon().build();
    }

    private Optional<KlageAvvistÅrsak> vurderOgLagreFormkrav(KlageFormkravAksjonspunktDto dto, Behandling behandling, KlageResultatEntitet klageResultat, KlageVurdertAv vurdertAv) {
        var gjelderVedtak = dto.hentpåKlagdBehandlingId() != null;
        if (dto.erTilbakekreving()) {
            klageVurderingTjeneste.oppdaterKlageMedPåklagetEksternBehandlingUuid(behandling.getId(), dto.getKlageTilbakekreving().getPåklagdEksternBehandlingUuid());
        } else if (gjelderVedtak || dto.hentpåKlagdBehandlingId() == null && klageResultat.getPåKlagdBehandlingId().isPresent()) {
            klageVurderingTjeneste.oppdaterKlageMedPåklagetBehandling(behandling.getId(), dto.hentpåKlagdBehandlingId());
        }

        var builder = klageVurderingTjeneste.hentKlageFormkravBuilder(behandling, vurdertAv)
            .medErKlagerPart(dto.erKlagerPart())
            .medErFristOverholdt(dto.erFristOverholdt())
            .medErKonkret(dto.erKonkret())
            .medErSignert(dto.erSignert())
            .medErFristOverholdt(dto.erFristOverholdt())
            .medBegrunnelse(dto.getBegrunnelse())
            .medGjelderVedtak(gjelderVedtak)
            .medKlageResultat(klageResultat);

        klageVurderingTjeneste.lagreFormkrav(behandling, builder);
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

    private void lagreKlageVurderingResultatMedAvvistKlage(Behandling klageBehandling, KlageVurdertAv vurdertAv) {
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(klageBehandling, vurdertAv);
        klageVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(klageBehandling, builder.medKlageVurdering(KlageVurdering.AVVIS_KLAGE), vurdertAv);
    }

    private void opprettHistorikkinnslag(Behandling klageBehandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                         KlageFormkravAksjonspunktDto formkravDto, Optional<KlageFormkravEntitet> klageFormkrav,
                                         KlageResultatEntitet klageResultat) {
        var historikkinnslagType = erNfpAksjonspunt(aksjonspunktDefinisjon) ? HistorikkinnslagType.KLAGE_BEH_NFP
            : HistorikkinnslagType.KLAGE_BEH_NK;
        var skjermlenkeType = getSkjermlenkeType(aksjonspunktDefinisjon);
        var historiebygger = historikkApplikasjonTjeneste.tekstBuilder();
        historiebygger.medSkjermlenke(skjermlenkeType).medBegrunnelse(formkravDto.getBegrunnelse());
        if (klageFormkrav.isEmpty()) {
            settOppHistorikkFelter(historiebygger, formkravDto);
        } else {
            finnOgSettOppEndredeHistorikkFelter(klageFormkrav.get(), historiebygger, formkravDto, klageResultat);
        }
        historikkApplikasjonTjeneste.opprettHistorikkInnslag(klageBehandling.getId(),historikkinnslagType);
    }

    private String hentPåklagdBehandlingTekst(Long behandlingId) {
        if (behandlingId == null) {
            return "Ikke påklagd et vedtak";
        }
        var påKlagdBehandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtaksDatoPåklagdBehandling = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId)
            .map(BehandlingVedtak::getVedtaksdato);
        return påKlagdBehandling.getType().getNavn() + " " +
            vedtaksDatoPåklagdBehandling.map(this::formatDato).orElse("");
    }

    private String hentPåKlagdEksternBehandlingTekst(UUID påKlagdEksternBehandlingUuid, String behandlingType, LocalDate vedtakDato) {
        if (påKlagdEksternBehandlingUuid == null) {
            return "Ikke påklagd et vedtak";
        }
        return BehandlingType.fraKode(behandlingType).getNavn() + " " + formatDato(vedtakDato);
    }

    private void settOppHistorikkFelter(HistorikkInnslagTekstBuilder historiebygger, KlageFormkravAksjonspunktDto formkravDto) {
        var behandlingId = formkravDto.hentpåKlagdBehandlingId();
        var påKlagdBehandling = !formkravDto.erTilbakekreving() ? hentPåklagdBehandlingTekst(behandlingId) :
            hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(), formkravDto.getKlageTilbakekreving().getTilbakekrevingBehandlingType(), formkravDto.getKlageTilbakekreving().getTilbakekrevingVedtakDato());
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
            var lagretPåklagdEksternBehandlingUuid = klageResultat.getPåKlagdEksternBehandlingUuid().orElse(null);
            var lagretPåklagdBehandlingId = klageResultat.getPåKlagdBehandlingId().orElse(null);
            var klageTilbakekrevingDto = formkravDto.getKlageTilbakekreving();
            if (lagretPåklagdEksternBehandlingUuid != null) {
                lagHistorikkinnslagHvisForrigePåklagdEksternBehandlingUuidFinnes(historikkInnslagTekstBuilder, formkravDto, lagretPåklagdEksternBehandlingUuid, klageTilbakekrevingDto);

            } else if (lagretPåklagdBehandlingId != null) {
                lagHistorikkinnslagHvisForrigePåklagdBehandlingFinnes(historikkInnslagTekstBuilder, formkravDto, klageResultat, lagretPåklagdBehandlingId, klageTilbakekrevingDto);

            } else {
                historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                    null,
                    formkravDto.erTilbakekreving() ? hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(), klageTilbakekrevingDto.getTilbakekrevingBehandlingType(), klageTilbakekrevingDto.getTilbakekrevingVedtakDato())
                        : hentPåklagdBehandlingTekst(formkravDto.hentpåKlagdBehandlingId()));
            }
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

    private void lagHistorikkinnslagHvisForrigePåklagdBehandlingFinnes(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, KlageFormkravAksjonspunktDto formkravDto, KlageResultatEntitet klageResultat, Long lagretPåklagdBehandlingId, KlageTilbakekrevingDto klageTilbakekrevingDto) {
        if (formkravDto.hentpåKlagdEksternBehandlingUuId() != null) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåklagdBehandlingTekst(lagretPåklagdBehandlingId),
                hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(), klageTilbakekrevingDto.getTilbakekrevingBehandlingType(), klageTilbakekrevingDto.getTilbakekrevingVedtakDato()));
        } else {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåklagdBehandlingTekst(klageResultat.getPåKlagdBehandlingId().orElse(null)),
                hentPåklagdBehandlingTekst(formkravDto.hentpåKlagdBehandlingId()));
        }
    }

    private void lagHistorikkinnslagHvisForrigePåklagdEksternBehandlingUuidFinnes(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, KlageFormkravAksjonspunktDto formkravDto, UUID lagretPåklagdEksternBehandlingUuid, KlageTilbakekrevingDto klageTilbakekrevingDto) {
        var tilbakekrevingVedtakDto = fptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(lagretPåklagdEksternBehandlingUuid);
        if (formkravDto.hentpåKlagdEksternBehandlingUuId() != null) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåKlagdEksternBehandlingTekst(lagretPåklagdEksternBehandlingUuid, tilbakekrevingVedtakDto.getTilbakekrevingBehandlingType(), tilbakekrevingVedtakDto.getTilbakekrevingVedtakDato()),
                hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(), klageTilbakekrevingDto.getTilbakekrevingBehandlingType(), klageTilbakekrevingDto.getTilbakekrevingVedtakDato()));
        } else {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåKlagdEksternBehandlingTekst(lagretPåklagdEksternBehandlingUuid, tilbakekrevingVedtakDto.getTilbakekrevingBehandlingType(), tilbakekrevingVedtakDto.getTilbakekrevingVedtakDato()),
                hentPåklagdBehandlingTekst(formkravDto.hentpåKlagdBehandlingId()));
        }
    }

    private KlageVurdertAv getKlageVurdertAv(AksjonspunktDefinisjon apdef) {
        return VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(apdef) ? KlageVurdertAv.NFP : KlageVurdertAv.NK;
    }

    private SkjermlenkeType getSkjermlenkeType(AksjonspunktDefinisjon apDef) {
        return VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(apDef) ? SkjermlenkeType.FORMKRAV_KLAGE_NFP : SkjermlenkeType.FORMKRAV_KLAGE_KA;
    }

    private boolean erVedtakOppdatert(KlageResultatEntitet klageResultat, KlageFormkravAksjonspunktDto formkravDto) {
        var lagretBehandlingId = klageResultat.getPåKlagdBehandlingId().orElse(null);
        var lagretBehandlingUuid = klageResultat.getPåKlagdEksternBehandlingUuid().orElse(null);

        return !Objects.equals(lagretBehandlingId, formkravDto.hentpåKlagdBehandlingId()) || !Objects.equals(lagretBehandlingUuid, formkravDto.hentpåKlagdEksternBehandlingUuId());
    }

    private boolean erNfpAksjonspunt(AksjonspunktDefinisjon apDef) {
        return Objects.equals(KlageVurdertAv.NFP, getKlageVurdertAv(apDef));
    }

    private String formatDato(LocalDate dato) {
        return dato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
