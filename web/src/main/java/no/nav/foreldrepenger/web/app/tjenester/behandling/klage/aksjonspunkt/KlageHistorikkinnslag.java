package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;

@ApplicationScoped
public class KlageHistorikkinnslag {

    private static final String IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST = "Ikke påklagd et vedtak";

    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private Historikkinnslag2Repository historikkinnslag2Repository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    private FptilbakeRestKlient fptilbakeRestKlient;

    KlageHistorikkinnslag() {
        // for CDI proxy
    }

    @Inject
    public KlageHistorikkinnslag(HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                 Historikkinnslag2Repository historikkinnslag2Repository,
                                 BehandlingRepository behandlingRepository,
                                 BehandlingVedtakRepository behandlingVedtakRepository,
                                 FptilbakeRestKlient fptilbakeRestKlient) {
        this.historikkinnslag2Repository = historikkinnslag2Repository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
    }

    public void opprettHistorikkinnslagFormkrav(Behandling klageBehandling,
                                                AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                                KlageFormKravLagreDto formkravDto,
                                                Optional<KlageFormkravEntitet> klageFormkrav,
                                                KlageResultatEntitet klageResultat,
                                                String begrunnelse) {
        var historikkinnslagType = erNfpAksjonspunt(aksjonspunktDefinisjon) ? HistorikkinnslagType.KLAGE_BEH_NFP : HistorikkinnslagType.KLAGE_BEH_NK;
        var skjermlenkeType = getSkjermlenkeType(aksjonspunktDefinisjon);
        var historiebygger = historikkApplikasjonTjeneste.tekstBuilder();
        historiebygger.medSkjermlenke(skjermlenkeType).medBegrunnelse(begrunnelse);
        if (klageFormkrav.isEmpty()) {
            settOppHistorikkFelter(historiebygger, formkravDto);
        } else {
            finnOgSettOppEndredeHistorikkFelter(klageFormkrav.get(), historiebygger, formkravDto, klageResultat);
        }
        historikkApplikasjonTjeneste.opprettHistorikkInnslag(klageBehandling.getId(), historikkinnslagType);
    }

    public void opprettHistorikkinnslagVurdering(Behandling behandling,
                                                 AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                                 KlageVurderingLagreDto dto,
                                                 String begrunnelse) {
        var klageVurdering = dto.getKlageVurdering();
        var klageVurderingOmgjør = dto.getKlageVurderingOmgjoer() != null ? dto.getKlageVurderingOmgjoer() : null;
        var erNfpAksjonspunkt = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.equals(aksjonspunktDefinisjon);

        var resultat = KlageVurderingTjeneste.historikkResultatForKlageVurdering(klageVurdering,
            erNfpAksjonspunkt ? KlageVurdertAv.NFP : KlageVurdertAv.NK, klageVurderingOmgjør);
        var tekstlinjer = new ArrayList<String>();
        if (erNfpAksjonspunkt && resultat != null) {
            tekstlinjer.add(new HistorikkinnslagTekstlinjeBuilder().til("Resultat", resultat.getNavn()).build());
        }
        var årsak = dto.getKlageMedholdArsak();
        if (årsak != null) {
            tekstlinjer.add(new HistorikkinnslagTekstlinjeBuilder().til("Årsak til omgjøring", årsak.getNavn()).build());
        }
        tekstlinjer.add(begrunnelse);

        var skjermlenkeType = erNfpAksjonspunkt ? SkjermlenkeType.KLAGE_BEH_NFP : SkjermlenkeType.KLAGE_BEH_NK;
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(skjermlenkeType)
            .medTekstlinjerString(tekstlinjer)
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    private String hentPåklagdBehandlingTekst(Long behandlingId) {
        if (behandlingId == null) {
            return IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST;
        }
        var påKlagdBehandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtaksDatoPåklagdBehandling = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(påKlagdBehandling.getId())
            .map(BehandlingVedtak::getVedtaksdato);
        return påKlagdBehandling.getType().getNavn() + " " + vedtaksDatoPåklagdBehandling.map(this::formatDato).orElse("");
    }

    private String hentPåklagdBehandlingTekst(UUID behandlingUuid) {
        return hentPåklagdBehandlingTekst(behandlingUuid == null ? null : behandlingRepository.hentBehandling(behandlingUuid).getId());
    }

    private String hentPåKlagdEksternBehandlingTekst(UUID påKlagdEksternBehandlingUuid, String behandlingType, LocalDate vedtakDato) {
        if (påKlagdEksternBehandlingUuid == null) {
            return IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST;
        }
        return BehandlingType.fraKode(behandlingType).getNavn() + " " + formatDato(vedtakDato);
    }

    private void settOppHistorikkFelter(HistorikkInnslagTekstBuilder historiebygger, KlageFormKravLagreDto formkravDto) {
        var behandlingId = formkravDto.påKlagdBehandlingUuid();
        var påKlagdBehandling = !formkravDto.erTilbakekreving() ? hentPåklagdBehandlingTekst(behandlingId) : hentPåKlagdEksternBehandlingTekst(
            formkravDto.hentpåKlagdEksternBehandlingUuId(), formkravDto.klageTilbakekreving().tilbakekrevingBehandlingType(),
            formkravDto.klageTilbakekreving().tilbakekrevingVedtakDato());
        historiebygger.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID, null, påKlagdBehandling)
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGER_PART, null, formkravDto.erKlagerPart())
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGEFRIST_OVERHOLDT, null, formkravDto.erFristOverholdt())
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGEN_SIGNERT, null, formkravDto.erSignert())
            .medEndretFelt(HistorikkEndretFeltType.ER_KLAGE_KONKRET, null, formkravDto.erKonkret());

    }

    private void finnOgSettOppEndredeHistorikkFelter(KlageFormkravEntitet klageFormkrav,
                                                     HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder,
                                                     KlageFormKravLagreDto formkravDto,
                                                     KlageResultatEntitet klageResultat) {
        if (erVedtakOppdatert(klageResultat, formkravDto)) {
            var lagretPåklagdEksternBehandlingUuid = klageResultat.getPåKlagdEksternBehandlingUuid().orElse(null);
            var lagretPåklagdBehandlingId = klageResultat.getPåKlagdBehandlingId().orElse(null);
            var klageTilbakekrevingDto = formkravDto.klageTilbakekreving();
            if (lagretPåklagdEksternBehandlingUuid != null) {
                lagHistorikkinnslagHvisForrigePåklagdEksternBehandlingUuidFinnes(historikkInnslagTekstBuilder, formkravDto,
                    lagretPåklagdEksternBehandlingUuid, klageTilbakekrevingDto);

            } else if (lagretPåklagdBehandlingId != null) {
                lagHistorikkinnslagHvisForrigePåklagdBehandlingFinnes(historikkInnslagTekstBuilder, formkravDto, klageResultat,
                    lagretPåklagdBehandlingId, klageTilbakekrevingDto);

            } else {
                historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID, null,
                    formkravDto.erTilbakekreving() ? hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                        klageTilbakekrevingDto.tilbakekrevingBehandlingType(),
                        klageTilbakekrevingDto.tilbakekrevingVedtakDato()) : hentPåklagdBehandlingTekst(formkravDto.påKlagdBehandlingUuid()));
            }
        }
        if (klageFormkrav.erKlagerPart() != formkravDto.erKlagerPart()) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_KLAGER_PART, klageFormkrav.erKlagerPart(),
                formkravDto.erKlagerPart());
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

    private void lagHistorikkinnslagHvisForrigePåklagdBehandlingFinnes(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder,
                                                                       KlageFormKravLagreDto formkravDto,
                                                                       KlageResultatEntitet klageResultat,
                                                                       Long lagretPåklagdBehandlingId,
                                                                       KlageTilbakekrevingDto klageTilbakekrevingDto) {
        if (formkravDto.hentpåKlagdEksternBehandlingUuId() != null) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåklagdBehandlingTekst(lagretPåklagdBehandlingId),
                hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                    klageTilbakekrevingDto.tilbakekrevingBehandlingType(), klageTilbakekrevingDto.tilbakekrevingVedtakDato()));
        } else {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåklagdBehandlingTekst(klageResultat.getPåKlagdBehandlingId().orElse(null)),
                hentPåklagdBehandlingTekst(formkravDto.påKlagdBehandlingUuid()));
        }
    }

    private void lagHistorikkinnslagHvisForrigePåklagdEksternBehandlingUuidFinnes(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder,
                                                                                  KlageFormKravLagreDto formkravDto,
                                                                                  UUID lagretPåklagdEksternBehandlingUuid,
                                                                                  KlageTilbakekrevingDto klageTilbakekrevingDto) {
        var tilbakekrevingVedtakDto = fptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(lagretPåklagdEksternBehandlingUuid);
        if (formkravDto.hentpåKlagdEksternBehandlingUuId() != null) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåKlagdEksternBehandlingTekst(lagretPåklagdEksternBehandlingUuid, tilbakekrevingVedtakDto.tilbakekrevingBehandlingType(),
                    tilbakekrevingVedtakDto.tilbakekrevingVedtakDato()),
                hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                    klageTilbakekrevingDto.tilbakekrevingBehandlingType(), klageTilbakekrevingDto.tilbakekrevingVedtakDato()));
        } else {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID,
                hentPåKlagdEksternBehandlingTekst(lagretPåklagdEksternBehandlingUuid, tilbakekrevingVedtakDto.tilbakekrevingBehandlingType(),
                    tilbakekrevingVedtakDto.tilbakekrevingVedtakDato()), hentPåklagdBehandlingTekst(formkravDto.påKlagdBehandlingUuid()));
        }
    }

    private KlageVurdertAv getKlageVurdertAv(AksjonspunktDefinisjon apdef) {
        return VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(apdef) ? KlageVurdertAv.NFP : KlageVurdertAv.NK;
    }

    private SkjermlenkeType getSkjermlenkeType(AksjonspunktDefinisjon apDef) {
        return VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(apDef) ? SkjermlenkeType.FORMKRAV_KLAGE_NFP : SkjermlenkeType.FORMKRAV_KLAGE_KA;
    }

    private boolean erVedtakOppdatert(KlageResultatEntitet klageResultat, KlageFormKravLagreDto formkravDto) {
        var lagretBehandlingId = klageResultat.getPåKlagdBehandlingId().orElse(null);
        var lagretEkternBehandlingUuid = klageResultat.getPåKlagdEksternBehandlingUuid().orElse(null);

        var påKlagdBehandlingUuid = formkravDto.påKlagdBehandlingUuid();
        var påKlagdBehandlingId =
            formkravDto.erTilbakekreving() || påKlagdBehandlingUuid == null ? null : behandlingRepository.hentBehandling(påKlagdBehandlingUuid)
                .getId();

        return !Objects.equals(lagretBehandlingId, påKlagdBehandlingId) || !Objects.equals(lagretEkternBehandlingUuid,
            formkravDto.hentpåKlagdEksternBehandlingUuId());
    }

    private boolean erNfpAksjonspunt(AksjonspunktDefinisjon apDef) {
        return Objects.equals(KlageVurdertAv.NFP, getKlageVurdertAv(apDef));
    }

    private String formatDato(LocalDate dato) {
        return dato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
