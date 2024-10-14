package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagDokumentLinkDto;

public class HistorikkV2Adapter {

    public static HistorikkinnslagDtoV2 map(Historikkinnslag h, UUID behandlingUUID) {
        return switch (h.getType()) {
            case BEH_GJEN,
                 KØET_BEH_GJEN,
                 BEH_MAN_GJEN,
                 BEH_STARTET,
                 BEH_STARTET_PÅ_NYTT,
                 // BEH_STARTET_FORFRA,  finnes ikke lenger? fptilbike?
                 VEDLEGG_MOTTATT,
                 BREV_SENT, BREV_BESTILT,
                 MIN_SIDE_ARBEIDSGIVER,
                 REVURD_OPPR,
                 REGISTRER_PAPIRSØK,
                 MANGELFULL_SØKNAD,
                 INNSYN_OPPR,
                 VRS_REV_IKKE_SNDT,
                 NYE_REGOPPLYSNINGER,
                 BEH_AVBRUTT_VUR,
                 BEH_OPPDATERT_NYE_OPPL,
                 SPOLT_TILBAKE,
                 // TILBAKEKREVING_OPPR, fptilbake
                 MIGRERT_FRA_INFOTRYGD,
                 MIGRERT_FRA_INFOTRYGD_FJERNET,
                 ANKEBEH_STARTET,
                 KLAGEBEH_STARTET,
                 ENDRET_DEKNINGSGRAD,
                 OPPGAVE_VEDTAK -> fraMaltype1(h, behandlingUUID);
            case FORSLAG_VEDTAK,
                 FORSLAG_VEDTAK_UTEN_TOTRINN,
                 VEDTAK_FATTET,
                 // VEDTAK_FATTET_AUTOMATISK, que?
                 UENDRET_UTFALL,
                 REGISTRER_OM_VERGE -> fraMaltype2(h, behandlingUUID);
            case SAK_GODKJENT,
                 FAKTA_ENDRET,
                 KLAGE_BEH_NK,
                 KLAGE_BEH_NFP,
                 BYTT_ENHET,
                 UTTAK,
                 TERMINBEKREFTELSE_UGYLDIG,
                 ANKE_BEH -> fraMalType5(h, behandlingUUID);

        };
    }

    private static HistorikkinnslagDtoV2 fraMalType5(Historikkinnslag h, UUID behandlingUUID) {
        var skjermlenke = h.getHistorikkinnslagDeler().stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .toList();

        var hendelseTekst = h.getHistorikkinnslagDeler().stream()
            .flatMap(del -> del.getHendelse().stream())
            .map(HistorikkinnslagFelt::getNavn)
            .map(HistorikkinnslagType::fraKode)
            .map(HistorikkinnslagType::getNavn)
            .toList();
        var resultatTekst = h.getHistorikkinnslagDeler().stream()
            .flatMap(del -> del.getResultat().stream())
            .map(HistorikkV2Adapter::fraHistorikkResultat)
            .toList();
        return null;
    }


    private static String fraHistorikkResultat(String type) {
        var historikkResultatType = HistorikkResultatType.valueOf(type);
        return switch (historikkResultatType) {
            case AVVIS_KLAGE -> "Klagen er avvist";
            case MEDHOLD_I_KLAGE -> "Vedtaket er omgjort";
            case OPPHEVE_VEDTAK -> "Vedtaket er opphevet";
            case OPPRETTHOLDT_VEDTAK -> "Vedtaket er opprettholdt";
            case STADFESTET_VEDTAK -> "Vedtaket er stadfestet";
            case BEREGNET_AARSINNTEKT -> "Grunnlag for beregnet årsinntekt";
            case UTFALL_UENDRET -> "Overstyrt vurdering: Utfall er uendret";
            case DELVIS_MEDHOLD_I_KLAGE -> "Vedtaket er delvis omgjort";
            case KLAGE_HJEMSENDE_UTEN_OPPHEVE -> "Behandling er hjemsendt";
            case UGUNST_MEDHOLD_I_KLAGE -> "Vedtaket er omgjort til ugunst";
            case OVERSTYRING_FAKTA_UTTAK -> "Overstyrt vurdering:";
            case ANKE_AVVIS -> historikkResultatType.getNavn(); // Ikke i frontend
            case ANKE_OMGJOER -> historikkResultatType.getNavn(); // Ikke i frontend
            case ANKE_OPPHEVE_OG_HJEMSENDE -> historikkResultatType.getNavn(); // Ikke i frontend
            case ANKE_HJEMSENDE -> historikkResultatType.getNavn(); // Ikke i frontend
            case ANKE_STADFESTET_VEDTAK -> historikkResultatType.getNavn(); // Ikke i frontend
            case ANKE_DELVIS_OMGJOERING_TIL_GUNST -> historikkResultatType.getNavn(); // Ikke i frontend
            case ANKE_TIL_UGUNST -> historikkResultatType.getNavn(); // Ikke i frontend
            case ANKE_TIL_GUNST -> historikkResultatType.getNavn(); // Ikke i frontend
            default -> historikkResultatType.getNavn();
        };
    }



    private static HistorikkinnslagDtoV2 fraMaltype2(Historikkinnslag h, UUID behandlingUUID) {

        return null // TODO:
    }


    private static HistorikkinnslagDtoV2 fraMaltype1(Historikkinnslag innslag, UUID behandlingUUID) {
        var historikkinnslagDel = innslag.getHistorikkinnslagDeler().getFirst();
        var skjermlenke = historikkinnslagDel.getSkjermlenke().map(SkjermlenkeType::fraKode).orElse(null);
        var lenker = tilDto(innslag.getDokumentLinker());
        var begrunnelsetekst = historikkinnslagDel.getBegrunnelseFelt()
            .flatMap(HistorikkV2Adapter::finnÅrsakKodeListe)
            .map(Kodeverdi::getNavn)
            .orElseGet(() -> historikkinnslagDel.getBegrunnelse().orElse(null));

        return new HistorikkinnslagDtoV2(
            behandlingUUID,
            HistorikkinnslagDtoV2.HistorikkAktørDto.fra(innslag.getAktør(), innslag.getOpprettetAv()),
            skjermlenke,
            innslag.getOpprettetTidspunkt(),
            lenker,
            innslag.getType().getNavn(),
            begrunnelsetekst
        );
    }

    private static List<HistorikkInnslagDokumentLinkDto> tilDto(List<HistorikkinnslagDokumentLink> dokumentLinker) {
        if (dokumentLinker == null) {
            return null;
        }
        return dokumentLinker.stream()
            .map(d -> tilDto(d, null, null)) // TODO: husks aktiv journalpost og full URI
            .toList();
    }

    private static HistorikkInnslagDokumentLinkDto tilDto(HistorikkinnslagDokumentLink lenke, List<JournalpostId> journalPosterForSak, URI dokumentPath) {
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(lenke.getLinkTekst());
        dto.setUtgått(aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak));
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        if (lenke.getJournalpostId().getVerdi() != null && lenke.getDokumentId() != null) {
            var builder = UriBuilder.fromUri(dokumentPath)
                .queryParam("journalpostId", lenke.getJournalpostId().getVerdi())
                .queryParam("dokumentId", lenke.getDokumentId());
            dto.setUrl(builder.build());
        }
        return dto;
    }

    private static boolean aktivJournalPost(JournalpostId journalpostId, List<JournalpostId> journalPosterForSak) {
        return journalPosterForSak.stream().filter(ajp -> Objects.equals(ajp, journalpostId)).findFirst().isEmpty();
    }


    // Fra HistorikkinnslagDelTo
    private static Optional<Kodeverdi> finnÅrsakKodeListe(HistorikkinnslagFelt aarsak) {

        var aarsakVerdi = aarsak.getTilVerdi();
        if (Objects.equals("-", aarsakVerdi)) {
            return Optional.empty();
        }
        if (aarsak.getKlTilVerdi() == null) {
            return Optional.empty();
        }

        var kodeverdiMap = HistorikkInnslagTekstBuilder.KODEVERK_KODEVERDI_MAP.get(aarsak.getKlTilVerdi());
        if (kodeverdiMap == null) {
            throw new IllegalStateException("Har ikke støtte for HistorikkinnslagFelt#klTilVerdi=" + aarsak.getKlTilVerdi());
        }
        return Optional.ofNullable(kodeverdiMap.get(aarsakVerdi));
    }
}
