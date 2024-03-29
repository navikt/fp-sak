package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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

    private static final String IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST = "Ikke påklagd et vedtak";

    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private FptilbakeRestKlient fptilbakeRestKlient;

    KlageFormkravOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KlageFormkravOppdaterer(KlageVurderingTjeneste klageVurderingTjeneste,
                                   HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   BehandlingVedtakRepository behandlingVedtakRepository,
                                   FptilbakeRestKlient fptilbakeRestKlient,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(KlageFormkravAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var apDefFormkrav = dto.getAksjonspunktDefinisjon();
        var klageVurdertAv = getKlageVurdertAv(apDefFormkrav);

        var klageBehandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var klageResultat = klageVurderingTjeneste.hentEvtOpprettKlageResultat(klageBehandling);

        if (KlageVurdertAv.NK.equals(klageVurdertAv)) {
            throw new IllegalArgumentException("KA Formkrav skal ikke lenger forekomme");
        }

        var klageFormkrav = klageVurderingTjeneste.hentKlageFormkrav(klageBehandling, klageVurdertAv);

        opprettHistorikkinnslag(klageBehandling, apDefFormkrav, dto, klageFormkrav, klageResultat);
        var optionalAvvistÅrsak = vurderOgLagreFormkrav(dto, klageBehandling, klageResultat, klageVurdertAv);
        if (optionalAvvistÅrsak.isPresent()) {
            lagreKlageVurderingResultatMedAvvistKlage(klageBehandling, klageVurdertAv, dto.fritekstTilBrev() != null ? dto.fritekstTilBrev() : null);
            return OppdateringResultat.medFremoverHoppTotrinn(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_VEDTAK);
        }
        //Må fjerne fritekst om det ble lagret i formkrav-vurderingen
        klageVurderingTjeneste.hentKlageVurderingResultat(klageBehandling, klageVurdertAv).ifPresent(klageVurderingResultat -> {
            if (klageVurderingResultat.getFritekstTilBrev() != null) {
                var vurderingResultatBuilder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(klageBehandling, klageVurdertAv).medFritekstTilBrev(null);
                klageVurderingTjeneste.lagreKlageVurderingResultat(klageBehandling, vurderingResultatBuilder, klageVurdertAv);
            }
        });
        klageBehandling.getÅpentAksjonspunktMedDefinisjonOptional(apDefFormkrav)
            .filter(Aksjonspunkt::isToTrinnsBehandling)
            .ifPresent(ap -> fjernToTrinnsbehandling(klageBehandling, ap));

        return OppdateringResultat.utenTransisjon().build();
    }

    private void fjernToTrinnsbehandling(Behandling behandling, Aksjonspunkt aksjonspunkt) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.setAksjonspunktToTrinn(kontekst, aksjonspunkt, false);
    }

    private Optional<KlageAvvistÅrsak> vurderOgLagreFormkrav(KlageFormkravAksjonspunktDto dto,
                                                             Behandling behandling,
                                                             KlageResultatEntitet klageResultat,
                                                             KlageVurdertAv vurdertAv) {
        if (dto.erTilbakekreving()) {
            klageVurderingTjeneste.oppdaterKlageMedPåklagetEksternBehandlingUuid(behandling.getId(),
                dto.getKlageTilbakekreving().tilbakekrevingUuid());
            BehandlingÅrsak.builder(BehandlingÅrsakType.KLAGE_TILBAKEBETALING).buildFor(behandling);
        } else {
            var påKlagdBehandlingUuid = dto.hentPåKlagdBehandlingUuid();
            if (påKlagdBehandlingUuid != null || dto.hentpåKlagdEksternBehandlingUuId() == null
                && klageResultat.getPåKlagdBehandlingId().isPresent()) {
                klageVurderingTjeneste.oppdaterKlageMedPåklagetBehandling(behandling, påKlagdBehandlingUuid);
            }
        }

        var builder = klageVurderingTjeneste.hentKlageFormkravBuilder(behandling, vurdertAv)
            .medErKlagerPart(dto.erKlagerPart())
            .medErFristOverholdt(dto.erFristOverholdt())
            .medErKonkret(dto.erKonkret())
            .medErSignert(dto.erSignert())
            .medErFristOverholdt(dto.erFristOverholdt())
            .medBegrunnelse(dto.getBegrunnelse())
            .medGjelderVedtak(dto.hentPåKlagdBehandlingUuid() != null)
            .medKlageResultat(klageResultat);

        klageVurderingTjeneste.lagreFormkrav(behandling, builder);
        return utledAvvistÅrsak(dto, dto.hentPåKlagdBehandlingUuid() != null);

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

    private void lagreKlageVurderingResultatMedAvvistKlage(Behandling klageBehandling, KlageVurdertAv vurdertAv, String fritekstTilBrev) {
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(klageBehandling, vurdertAv)
            .medKlageVurdering(KlageVurdering.AVVIS_KLAGE)
            .medFritekstTilBrev(fritekstTilBrev);
        klageVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(klageBehandling, builder, vurdertAv);
    }

    private void opprettHistorikkinnslag(Behandling klageBehandling,
                                         AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                         KlageFormkravAksjonspunktDto formkravDto,
                                         Optional<KlageFormkravEntitet> klageFormkrav,
                                         KlageResultatEntitet klageResultat) {
        var historikkinnslagType = erNfpAksjonspunt(
            aksjonspunktDefinisjon) ? HistorikkinnslagType.KLAGE_BEH_NFP : HistorikkinnslagType.KLAGE_BEH_NK;
        var skjermlenkeType = getSkjermlenkeType(aksjonspunktDefinisjon);
        var historiebygger = historikkApplikasjonTjeneste.tekstBuilder();
        historiebygger.medSkjermlenke(skjermlenkeType).medBegrunnelse(formkravDto.getBegrunnelse());
        if (klageFormkrav.isEmpty()) {
            settOppHistorikkFelter(historiebygger, formkravDto);
        } else {
            finnOgSettOppEndredeHistorikkFelter(klageFormkrav.get(), historiebygger, formkravDto, klageResultat);
        }
        historikkApplikasjonTjeneste.opprettHistorikkInnslag(klageBehandling.getId(), historikkinnslagType);
    }

    private String hentPåklagdBehandlingTekst(Long behandlingId) {
        if (behandlingId == null) {
            return IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST;
        }
        var påKlagdBehandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtaksDatoPåklagdBehandling = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(påKlagdBehandling.getId())
            .map(BehandlingVedtak::getVedtaksdato);
        return påKlagdBehandling.getType().getNavn() + " " + vedtaksDatoPåklagdBehandling.map(this::formatDato)
            .orElse("");
    }

    private String hentPåklagdBehandlingTekst(UUID behandlingUuid) {
        return hentPåklagdBehandlingTekst(behandlingUuid == null ? null : behandlingRepository.hentBehandling(behandlingUuid).getId());
    }

    private String hentPåKlagdEksternBehandlingTekst(UUID påKlagdEksternBehandlingUuid,
                                                     String behandlingType,
                                                     LocalDate vedtakDato) {
        if (påKlagdEksternBehandlingUuid == null) {
            return IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST;
        }
        return BehandlingType.fraKode(behandlingType).getNavn() + " " + formatDato(vedtakDato);
    }

    private void settOppHistorikkFelter(HistorikkInnslagTekstBuilder historiebygger,
                                        KlageFormkravAksjonspunktDto formkravDto) {
        var behandlingId = formkravDto.hentPåKlagdBehandlingUuid();
        var påKlagdBehandling = !formkravDto.erTilbakekreving() ? hentPåklagdBehandlingTekst(behandlingId)
            : hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
            formkravDto.getKlageTilbakekreving().tilbakekrevingBehandlingType(),
            formkravDto.getKlageTilbakekreving().tilbakekrevingVedtakDato());
        historiebygger.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID, null, påKlagdBehandling)
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGER_PART, null, formkravDto.erKlagerPart())
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGEFRIST_OVERHOLDT, null, formkravDto.erFristOverholdt())
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGEN_SIGNERT, null, formkravDto.erSignert())
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGE_KONKRET, null, formkravDto.erKonkret());

    }

    private void finnOgSettOppEndredeHistorikkFelter(KlageFormkravEntitet klageFormkrav,
                                                     HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder,
                                                     KlageFormkravAksjonspunktDto formkravDto,
                                                     KlageResultatEntitet klageResultat) {
        if (erVedtakOppdatert(klageResultat, formkravDto)) {
            var lagretPåklagdEksternBehandlingUuid = klageResultat.getPåKlagdEksternBehandlingUuid().orElse(null);
            var lagretPåklagdBehandlingId = klageResultat.getPåKlagdBehandlingId().orElse(null);
            var klageTilbakekrevingDto = formkravDto.getKlageTilbakekreving();
            if (lagretPåklagdEksternBehandlingUuid != null) {
                lagHistorikkinnslagHvisForrigePåklagdEksternBehandlingUuidFinnes(historikkInnslagTekstBuilder,
                    formkravDto, lagretPåklagdEksternBehandlingUuid, klageTilbakekrevingDto);

            } else if (lagretPåklagdBehandlingId != null) {
                lagHistorikkinnslagHvisForrigePåklagdBehandlingFinnes(historikkInnslagTekstBuilder, formkravDto,
                    klageResultat, lagretPåklagdBehandlingId, klageTilbakekrevingDto);

            } else {
                historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID, null,
                    formkravDto.erTilbakekreving() ? hentPåKlagdEksternBehandlingTekst(
                        formkravDto.hentpåKlagdEksternBehandlingUuId(),
                        klageTilbakekrevingDto.tilbakekrevingBehandlingType(),
                        klageTilbakekrevingDto.tilbakekrevingVedtakDato()) : hentPåklagdBehandlingTekst(
                        formkravDto.hentPåKlagdBehandlingUuid()));
            }
        }
        if (klageFormkrav.erKlagerPart() != formkravDto.erKlagerPart()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGER_PART,
                klageFormkrav.erKlagerPart(), formkravDto.erKlagerPart());
        }
        if (klageFormkrav.erFristOverholdt() != formkravDto.erFristOverholdt()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGEFRIST_OVERHOLDT,
                klageFormkrav.erFristOverholdt(), formkravDto.erFristOverholdt());
        }
        if (klageFormkrav.erSignert() != formkravDto.erSignert()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGEN_SIGNERT,
                klageFormkrav.erSignert(), formkravDto.erSignert());
        }
        if (klageFormkrav.erKonkret() != formkravDto.erKonkret()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGE_KONKRET,
                klageFormkrav.erKonkret(), formkravDto.erKonkret());
        }
    }

    private void lagHistorikkinnslagHvisForrigePåklagdBehandlingFinnes(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder,
                                                                       KlageFormkravAksjonspunktDto formkravDto,
                                                                       KlageResultatEntitet klageResultat,
                                                                       Long lagretPåklagdBehandlingId,
                                                                       KlageTilbakekrevingDto klageTilbakekrevingDto) {
        if (formkravDto.hentpåKlagdEksternBehandlingUuId() != null) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåklagdBehandlingTekst(lagretPåklagdBehandlingId),
                hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                    klageTilbakekrevingDto.tilbakekrevingBehandlingType(),
                    klageTilbakekrevingDto.tilbakekrevingVedtakDato()));
        } else {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåklagdBehandlingTekst(klageResultat.getPåKlagdBehandlingId().orElse(null)),
                hentPåklagdBehandlingTekst(formkravDto.hentPåKlagdBehandlingUuid()));
        }
    }

    private void lagHistorikkinnslagHvisForrigePåklagdEksternBehandlingUuidFinnes(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder,
                                                                                  KlageFormkravAksjonspunktDto formkravDto,
                                                                                  UUID lagretPåklagdEksternBehandlingUuid,
                                                                                  KlageTilbakekrevingDto klageTilbakekrevingDto) {
        var tilbakekrevingVedtakDto = fptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(
            lagretPåklagdEksternBehandlingUuid);
        if (formkravDto.hentpåKlagdEksternBehandlingUuId() != null) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåKlagdEksternBehandlingTekst(lagretPåklagdEksternBehandlingUuid,
                    tilbakekrevingVedtakDto.tilbakekrevingBehandlingType(),
                    tilbakekrevingVedtakDto.tilbakekrevingVedtakDato()),
                hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                    klageTilbakekrevingDto.tilbakekrevingBehandlingType(),
                    klageTilbakekrevingDto.tilbakekrevingVedtakDato()));
        } else {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåKlagdEksternBehandlingTekst(lagretPåklagdEksternBehandlingUuid,
                    tilbakekrevingVedtakDto.tilbakekrevingBehandlingType(),
                    tilbakekrevingVedtakDto.tilbakekrevingVedtakDato()),
                hentPåklagdBehandlingTekst(formkravDto.hentPåKlagdBehandlingUuid()));
        }
    }

    private KlageVurdertAv getKlageVurdertAv(AksjonspunktDefinisjon apdef) {
        return VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(apdef) ? KlageVurdertAv.NFP : KlageVurdertAv.NK;
    }

    private SkjermlenkeType getSkjermlenkeType(AksjonspunktDefinisjon apDef) {
        return VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(
            apDef) ? SkjermlenkeType.FORMKRAV_KLAGE_NFP : SkjermlenkeType.FORMKRAV_KLAGE_KA;
    }

    private boolean erVedtakOppdatert(KlageResultatEntitet klageResultat, KlageFormkravAksjonspunktDto formkravDto) {
        var lagretBehandlingId = klageResultat.getPåKlagdBehandlingId().orElse(null);
        var lagretEkternBehandlingUuid = klageResultat.getPåKlagdEksternBehandlingUuid().orElse(null);

        var påKlagdBehandlingUuid = formkravDto.hentPåKlagdBehandlingUuid();
        var påKlagdBehandlingId =
            formkravDto.erTilbakekreving() || påKlagdBehandlingUuid == null ? null : behandlingRepository.hentBehandling(påKlagdBehandlingUuid)
                .getId();

        return !Objects.equals(lagretBehandlingId, påKlagdBehandlingId) ||
            !Objects.equals(lagretEkternBehandlingUuid, formkravDto.hentpåKlagdEksternBehandlingUuId());
    }

    private boolean erNfpAksjonspunt(AksjonspunktDefinisjon apDef) {
        return Objects.equals(KlageVurdertAv.NFP, getKlageVurdertAv(apDef));
    }

    private String formatDato(LocalDate dato) {
        return dato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
