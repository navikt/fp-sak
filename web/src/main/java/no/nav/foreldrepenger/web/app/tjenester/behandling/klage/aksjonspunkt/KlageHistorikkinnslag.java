package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.produksjonsstyring.tilbakekreving.FptilbakeRestKlient;

@ApplicationScoped
public class KlageHistorikkinnslag {

    private static final String IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST = "Ikke påklagd et vedtak";
    public static final String PÅKLAGD_BEHANDLING = "Påklagd behandling";

    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    private FptilbakeRestKlient fptilbakeRestKlient;

    KlageHistorikkinnslag() {
        // for CDI proxy
    }

    @Inject
    public KlageHistorikkinnslag(HistorikkinnslagRepository historikkinnslagRepository,
                                 BehandlingRepository behandlingRepository,
                                 BehandlingVedtakRepository behandlingVedtakRepository,
                                 FptilbakeRestKlient fptilbakeRestKlient) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
    }

    public void opprettHistorikkinnslagFormkrav(Behandling klageBehandling,
                                                AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                                KlageFormKravLagreDto formkravDto,
                                                Optional<KlageFormkravEntitet> klageFormkrav,
                                                KlageResultatEntitet klageResultat,
                                                LocalDate mottattDato,
                                                String begrunnelse) {
        var skjermlenkeType = getSkjermlenkeType(aksjonspunktDefinisjon);

        var linjer = klageFormkrav.map(klageFormkravEntitet -> lagHistorikkLinjer(klageFormkravEntitet, mottattDato, formkravDto, klageResultat))
            .orElseGet(() -> lagHistorikkLinjer(mottattDato, formkravDto));
        var historikkinnslag = new Historikkinnslag.Builder().medTittel(skjermlenkeType)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(klageBehandling.getFagsakId())
            .medBehandlingId(klageBehandling.getId())
            .medLinjer(linjer)
            .addLinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
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
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        if (erNfpAksjonspunkt && resultat != null) {
            linjer.add(linjeBuilder().til("Resultat", resultat));
        }
        var årsak = dto.getKlageMedholdArsak();
        if (årsak != null) {
            linjer.add(linjeBuilder().til("Årsak til omgjøring", årsak.getNavn()));
        }

        var skjermlenkeType = erNfpAksjonspunkt ? SkjermlenkeType.KLAGE_BEH_NFP : SkjermlenkeType.KLAGE_BEH_NK;
        var historikkinnslag = new Historikkinnslag.Builder().medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(skjermlenkeType)
            .medLinjer(linjer)
            .addLinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private String hentPåklagdBehandlingTekst(Long behandlingId) {
        if (behandlingId == null) {
            return IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST;
        }
        var påKlagdBehandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtaksDatoPåklagdBehandling = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(påKlagdBehandling.getId())
            .map(BehandlingVedtak::getVedtaksdato);
        return påKlagdBehandling.getType().getNavn() + " " + vedtaksDatoPåklagdBehandling.map(dato -> format(dato)).orElse("");
    }

    private String hentPåklagdBehandlingTekst(UUID behandlingUuid) {
        return hentPåklagdBehandlingTekst(behandlingUuid == null ? null : behandlingRepository.hentBehandling(behandlingUuid).getId());
    }

    private String hentPåKlagdEksternBehandlingTekst(UUID påKlagdEksternBehandlingUuid, String behandlingType, LocalDate vedtakDato) {
        if (påKlagdEksternBehandlingUuid == null) {
            return IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST;
        }
        return BehandlingType.fraKode(behandlingType).getNavn() + " " + format(vedtakDato);
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkLinjer(LocalDate mottattDato, KlageFormKravLagreDto formkravDto) {
        var behandlingId = formkravDto.påKlagdBehandlingUuid();
        var påKlagdBehandling = !formkravDto.erTilbakekreving() ? hentPåklagdBehandlingTekst(behandlingId) : hentPåKlagdEksternBehandlingTekst(
            formkravDto.hentpåKlagdEksternBehandlingUuId(), formkravDto.klageTilbakekreving().tilbakekrevingBehandlingType(),
            formkravDto.klageTilbakekreving().tilbakekrevingVedtakDato());

        var linjer = new ArrayList<>(List.of(linjeBuilder().til(PÅKLAGD_BEHANDLING, påKlagdBehandling),
            linjeBuilder().til("Er klager part", formkravDto.erKlagerPart()),
            linjeBuilder().til("Er klagefrist overholdt", formkravDto.erFristOverholdt()),
            linjeBuilder().til("Er klagen signert", formkravDto.erSignert()),
            linjeBuilder().til("Er klagen konkret", formkravDto.erKonkret())));

        if (!Objects.equals(mottattDato, formkravDto.mottattDato())) {
            linjer.add(
                new HistorikkinnslagLinjeBuilder().fraTil("Klage mottatt dato", mottattDato, formkravDto.mottattDato()));
        }
        return linjer;
    }

    private static HistorikkinnslagLinjeBuilder linjeBuilder() {
        return new HistorikkinnslagLinjeBuilder();
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkLinjer(KlageFormkravEntitet klageFormkrav, LocalDate mottattDato,
                                            KlageFormKravLagreDto formkravDto,
                                            KlageResultatEntitet klageResultat) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        if (erVedtakOppdatert(klageResultat, formkravDto)) {
            var lagretPåklagdEksternBehandlingUuid = klageResultat.getPåKlagdEksternBehandlingUuid().orElse(null);
            var lagretPåklagdBehandlingId = klageResultat.getPåKlagdBehandlingId().orElse(null);
            var klageTilbakekrevingDto = formkravDto.klageTilbakekreving();
            if (lagretPåklagdEksternBehandlingUuid != null) {
                linjer.add(lagLinjeHvisForrigePåklagdEksternBehandlingUuidFinnes(formkravDto, lagretPåklagdEksternBehandlingUuid,
                    klageTilbakekrevingDto));

            } else if (lagretPåklagdBehandlingId != null) {
                linjer.add(
                    lagLinjeHvisForrigePåklagdBehandlingFinnes(formkravDto, klageResultat, lagretPåklagdBehandlingId, klageTilbakekrevingDto));

            } else {
                var tilVerdi = formkravDto.erTilbakekreving() ? hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                    klageTilbakekrevingDto.tilbakekrevingBehandlingType(),
                    klageTilbakekrevingDto.tilbakekrevingVedtakDato()) : hentPåklagdBehandlingTekst(formkravDto.påKlagdBehandlingUuid());
                linjer.add(new HistorikkinnslagLinjeBuilder().til(PÅKLAGD_BEHANDLING, tilVerdi));
            }
        }
        if (klageFormkrav.erKlagerPart() != formkravDto.erKlagerPart()) {
            linjer.add(
                new HistorikkinnslagLinjeBuilder().fraTil("Er klager part", klageFormkrav.erKlagerPart(), formkravDto.erKlagerPart()));
        }
        if (klageFormkrav.erFristOverholdt() != formkravDto.erFristOverholdt()) {
            linjer.add(new HistorikkinnslagLinjeBuilder().fraTil("Er klagefrist overholdt", klageFormkrav.erFristOverholdt(),
                formkravDto.erFristOverholdt()));
        }
        if (klageFormkrav.erSignert() != formkravDto.erSignert()) {
            linjer.add(
                new HistorikkinnslagLinjeBuilder().fraTil("Er klagen signert", klageFormkrav.erSignert(), formkravDto.erSignert()));

        }
        if (klageFormkrav.erKonkret() != formkravDto.erKonkret()) {
            linjer.add(
                new HistorikkinnslagLinjeBuilder().fraTil("Er klagen konkret", klageFormkrav.erKonkret(), formkravDto.erKonkret()));
        }
        if (!Objects.equals(mottattDato, formkravDto.mottattDato())) {
            linjer.add(
                new HistorikkinnslagLinjeBuilder().fraTil("Klage mottatt dato", mottattDato, formkravDto.mottattDato()));
        }
        return linjer;
    }

    private HistorikkinnslagLinjeBuilder lagLinjeHvisForrigePåklagdBehandlingFinnes(KlageFormKravLagreDto formkravDto,
                                                              KlageResultatEntitet klageResultat,
                                                              Long lagretPåklagdBehandlingId,
                                                              KlageTilbakekrevingDto klageTilbakekrevingDto) {
        if (formkravDto.hentpåKlagdEksternBehandlingUuId() != null) {
            var fraVerdi = hentPåklagdBehandlingTekst(lagretPåklagdBehandlingId);
            var tilVerdi = hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                klageTilbakekrevingDto.tilbakekrevingBehandlingType(), klageTilbakekrevingDto.tilbakekrevingVedtakDato());
            return HistorikkinnslagLinjeBuilder.fraTilEquals(PÅKLAGD_BEHANDLING, fraVerdi, tilVerdi);
        } else {
            var fraVerdi = hentPåklagdBehandlingTekst(klageResultat.getPåKlagdBehandlingId().orElse(null));
            var tilVerdi = hentPåklagdBehandlingTekst(formkravDto.påKlagdBehandlingUuid());
            return HistorikkinnslagLinjeBuilder.fraTilEquals(PÅKLAGD_BEHANDLING, fraVerdi, tilVerdi);
        }
    }

    private HistorikkinnslagLinjeBuilder lagLinjeHvisForrigePåklagdEksternBehandlingUuidFinnes(KlageFormKravLagreDto formkravDto,
                                                                         UUID lagretPåklagdEksternBehandlingUuid,
                                                                         KlageTilbakekrevingDto klageTilbakekrevingDto) {
        var tilbakekrevingVedtakDto = fptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(lagretPåklagdEksternBehandlingUuid);
        if (formkravDto.hentpåKlagdEksternBehandlingUuId() != null) {
            var fraVerdi = hentPåKlagdEksternBehandlingTekst(lagretPåklagdEksternBehandlingUuid,
                tilbakekrevingVedtakDto.tilbakekrevingBehandlingType(), tilbakekrevingVedtakDto.tilbakekrevingVedtakDato());
            var tilVerdi = hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                klageTilbakekrevingDto.tilbakekrevingBehandlingType(), klageTilbakekrevingDto.tilbakekrevingVedtakDato());
            return HistorikkinnslagLinjeBuilder.fraTilEquals(PÅKLAGD_BEHANDLING, fraVerdi, tilVerdi);
        } else {
            var fraVerdi = hentPåKlagdEksternBehandlingTekst(lagretPåklagdEksternBehandlingUuid,
                tilbakekrevingVedtakDto.tilbakekrevingBehandlingType(), tilbakekrevingVedtakDto.tilbakekrevingVedtakDato());
            var tilVerdi = hentPåklagdBehandlingTekst(formkravDto.påKlagdBehandlingUuid());
            return HistorikkinnslagLinjeBuilder.fraTilEquals(PÅKLAGD_BEHANDLING, fraVerdi, tilVerdi);
        }
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

}
