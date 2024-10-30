package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagDokumentLinkDto;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType.UTTAK_PERIODE_FOM;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType.UTTAK_PERIODE_TOM;

public class HistorikkV2Adapter {

    private static final Logger LOG = LoggerFactory.getLogger(HistorikkV2Adapter.class);

    public static HistorikkinnslagDtoV2 map(Historikkinnslag h, UUID behandlingUUID) {
        return switch (h.getType()) {
            case BEH_GJEN, KØET_BEH_GJEN, BEH_MAN_GJEN, BEH_STARTET, BEH_STARTET_PÅ_NYTT,
                 // BEH_STARTET_FORFRA,  finnes ikke lenger? fptilbike?
                 VEDLEGG_MOTTATT, BREV_SENT, BREV_BESTILT, MIN_SIDE_ARBEIDSGIVER, REVURD_OPPR, REGISTRER_PAPIRSØK, MANGELFULL_SØKNAD, INNSYN_OPPR,
                 VRS_REV_IKKE_SNDT, NYE_REGOPPLYSNINGER, BEH_AVBRUTT_VUR, BEH_OPPDATERT_NYE_OPPL, SPOLT_TILBAKE,
                 // TILBAKEKREVING_OPPR, fptilbake
                 MIGRERT_FRA_INFOTRYGD, MIGRERT_FRA_INFOTRYGD_FJERNET, ANKEBEH_STARTET, KLAGEBEH_STARTET, ENDRET_DEKNINGSGRAD, OPPGAVE_VEDTAK ->
                fraMaltype1(h, behandlingUUID);
            case FORSLAG_VEDTAK, FORSLAG_VEDTAK_UTEN_TOTRINN, VEDTAK_FATTET,
                 // VEDTAK_FATTET_AUTOMATISK, que?
                 UENDRET_UTFALL, REGISTRER_OM_VERGE -> fraMaltype2(h, behandlingUUID);
            case SAK_GODKJENT, FAKTA_ENDRET, KLAGE_BEH_NK, KLAGE_BEH_NFP, BYTT_ENHET, UTTAK, TERMINBEKREFTELSE_UGYLDIG, ANKE_BEH ->
                fraMalType5(h, behandlingUUID);
            case OVST_UTTAK, FASTSATT_UTTAK -> fraMaltype10(h, behandlingUUID);
            default -> null; //TODO fjerne default
        };
    }

    private static HistorikkinnslagDtoV2 fraMaltype10(Historikkinnslag h, UUID behandlingUUID) {
        var skjermlenke = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .findFirst();
        var tittel = "TODO";
        var tekster = new ArrayList<String>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var periodeFom = del.getOpplysninger().stream()
                .filter(o -> UTTAK_PERIODE_FOM.getKode().equals(o.getNavn()))
                .map(HistorikkinnslagFelt::getTilVerdi)
                .findFirst()
                .orElse("");

            var periodeTom = del.getOpplysninger().stream()
                .filter(o -> UTTAK_PERIODE_TOM.getKode().equals(o.getNavn()))
                .map(HistorikkinnslagFelt::getTilVerdi)
                .findFirst()
                .orElse("");

            String opplysningTekst = "";
            if (h.getType().equals(HistorikkinnslagType.OVST_UTTAK)) {
                opplysningTekst = String.format("<b>Overstyrt vurdering</b> av perioden %s - %s.", periodeFom, periodeTom);

            }
            if (h.getType().equals(HistorikkinnslagType.FASTSATT_UTTAK)) {
                opplysningTekst = String.format("<b>Manuell vurdering</b> av perioden %s - %s.", periodeFom, periodeTom);
            }

            var endretFelter = del.getEndredeFelt().stream()
                .map(HistorikkV2Adapter::fraEndretFeltMalType10)
                .toList();

            var begrunnelsetekst = h.getHistorikkinnslagDeler().stream().flatMap(d -> begrunnelseFraDel(d).stream()).toList();

            var body = new ArrayList<String>();
            body.add(opplysningTekst);
            body.addAll(endretFelter);
            body.addAll(begrunnelsetekst);
            tekster.addAll(body);
        }

        return new HistorikkinnslagDtoV2(
            behandlingUUID,
            HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            skjermlenke.orElse(null),
            h.getOpprettetTidspunkt(),
            null, // TODO
            tittel,
            tekster);
    }

    private static String fraEndretFeltMalType10(HistorikkinnslagFelt felt) {
        var tekst = "";
        var fieldName = kodeverdiTilStreng(HistorikkEndretFeltType.fraKode(felt.getNavn()), felt.getNavnVerdi());

        if (
            HistorikkEndretFeltType.UTTAK_TREKKDAGER.getKode().equals(felt.getNavn())
            && erTallVerdi(felt.getFraVerdi())
            && erTallVerdi(felt.getTilVerdi())
        )
        {
            var fraVerdi = Double.parseDouble(felt.getFraVerdi());
            var tilVerdi = Double.parseDouble(felt.getTilVerdi());
            var fraVerdiUker = (int) Math.floor(fraVerdi / 5);
            var fraVerdiDager = fraVerdi % 1 == 0 ? fraVerdi % 5 : (Math.round(fraVerdi % 5 * 10) / 10.0);
            var tilVerdiUker = (int) Math.floor(tilVerdi / 5);
            var tilVerdiDager = tilVerdi % 1 == 0 ? tilVerdi % 5 : (Math.round(tilVerdi % 5 * 10) / 10.0);

            tekst = String.format("<b>%s</b> er endret fra %s uker og %s dager til <b>%s uker og %s dager</b>", fieldName, fraVerdiUker,
                fraVerdiDager, tilVerdiUker, tilVerdiDager);

        } else {
            tekst = historikkFraTilVerdi(felt, fieldName);
        }

        return tekst;
    }

    private static String historikkFraTilVerdi(HistorikkinnslagFelt felt, String fieldName) {
        var fraVerdi = finnEndretFeltVerdi(felt, felt.getFraVerdi());
        var tilVerdi = finnEndretFeltVerdi(felt, felt.getTilVerdi());
        var tekstMeldingTil = String.format("<b>%s</b> er satt til <b>%s</b>", fieldName, tilVerdi);
        var tekstMeldingEndretFraTil = String.format("<b>%s</b> er endret fra %s til <b>%s</b>", fieldName, fraVerdi, tilVerdi);
        var tekst = fraVerdi != null ? tekstMeldingEndretFraTil : tekstMeldingTil;

        if (HistorikkEndretFeltType.UTTAK_PROSENT_UTBETALING.getKode().equals(felt.getNavn()) && fraVerdi != null) {
            tekst = String.format("<b>%s</b> er endret fra %s %% til <b>%s %%</b>", fieldName, fraVerdi, tilVerdi);
        } else if (HistorikkEndretFeltType.UTTAK_PROSENT_UTBETALING.getKode().equals(felt.getNavn())) {
            tekst = String.format("<b>%s</b> er satt til <b>%s%%</b>", fieldName, tilVerdi);
        } else if (HistorikkEndretFeltType.UTTAK_PERIODE_RESULTAT_TYPE.getKode().equals(felt.getNavn()) && "MANUELL_BEHANDLING".equals(felt.getFraVerdi())) {
            tekst = tekstMeldingTil;
        } else if (HistorikkEndretFeltType.UTTAK_PERIODE_RESULTAT_ÅRSAK.getKode().equals(felt.getNavn())
            || HistorikkEndretFeltType.UTTAK_GRADERING_AVSLAG_ÅRSAK.getKode().equals(felt.getNavn())) {

            if ("_".equals(felt.getTilVerdi())) {
                return "";
            } else if ("_".equals(felt.getFraVerdi())) {
                tekst = tekstMeldingTil;
            }
        } else if (HistorikkEndretFeltType.UTTAK_STØNADSKONTOTYPE.getKode().equals(felt.getNavn()) && "_".equals(felt.getFraVerdi())) {
            tekst = tekstMeldingTil;
        }

        return tekst;
    }

    // TODO Thao: Flytt denne til en util klasse
    public static boolean erTallVerdi(Object value) {
        return value instanceof Number;
    }

    private static HistorikkinnslagDtoV2 fraMalType5(Historikkinnslag h, UUID behandlingUUID) {
        var skjermlenke = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .findFirst();
        var lenker = tilDto(h.getDokumentLinker());
        var tittel = switch (h.getType()) {
            case KLAGE_BEH_NK, KLAGE_BEH_NFP, BYTT_ENHET, ANKE_BEH -> h.getType().getNavn();
            default -> null;
        };
        return new HistorikkinnslagDtoV2(behandlingUUID, HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            skjermlenke.orElse(null), h.getOpprettetTidspunkt(), lenker, tittel, lagTekstForMal5(h));
    }

    /**
     * Slik er det nå (dette er feil):
     * Hendelse 1
     * Hendelse 2
     * Resultat 1
     * Resultat 2
     *
     * Dette er riktig:
     * Hendelse 1
     * Resultat 1
     * Hendelse 2
     * Resultat 2
     */
    private static List<String> lagTekstForMal5(Historikkinnslag h) {
        var hendelseTekst = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getHendelse().stream())
            .map(HistorikkinnslagFelt::getNavn)
            .map(HistorikkinnslagType::fraKode)
            .map(HistorikkinnslagType::getNavn)
            .toList();

        var resultatTekst = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getResultat().stream())
            .map(HistorikkV2Adapter::fraHistorikkResultat)
            .toList();

        var gjeldendeFraInnslag = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getGjeldendeFraFelt().stream().map(felt -> tilGjeldendeFraInnslag(felt, del)))
            .toList();

        var søknadsperiode = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getAvklartSoeknadsperiode().stream())
            .map(HistorikkV2Adapter::fraSøknadsperiode)
            .toList();

        var tema = h.getHistorikkinnslagDeler().stream().flatMap(del -> del.getTema().stream()).map(HistorikkV2Adapter::fraTema).toList();

        var endretFelter = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getEndredeFelt().stream())
            .map(HistorikkV2Adapter::fraEndretFelt)
            .toList();

        var opplysninger = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getOpplysninger().stream())
            .map(HistorikkV2Adapter::fraOpplysning)
            .toList();

        var årsaktekst = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getAarsakFelt().stream())
            .flatMap(felt -> finnÅrsakKodeListe(felt).stream())
            .map(Kodeverdi::getNavn)
            .toList();

        var begrunnelsetekst = h.getHistorikkinnslagDeler().stream().flatMap(d -> begrunnelseFraDel(d).stream()).toList();

        var body = new ArrayList<>(hendelseTekst);
        body.addAll(hendelseTekst);
        body.addAll(resultatTekst);
        body.addAll(gjeldendeFraInnslag);
        body.addAll(søknadsperiode);
        body.addAll(tema);
        body.addAll(endretFelter);
        body.addAll(opplysninger);
        body.addAll(årsaktekst);
        body.addAll(begrunnelsetekst);
        return body;
    }

    private static String fraOpplysning(HistorikkinnslagFelt opplysning) {
        var historikkOpplysningType = HistorikkOpplysningType.fraKode(opplysning.getNavn());
        var tekst = switch (historikkOpplysningType) {
            case ANTALL_BARN -> "<b>Antall barn</b> som brukes i behandlingen: <b>{antallBarn}</b>";
            case TPS_ANTALL_BARN -> "Antall barn";
            case FODSELSDATO -> "Når ble barnet født?";
            case UTTAK_PERIODE_FOM -> historikkOpplysningType.getNavn(); // TODO: ingen feltid frontend?
            case UTTAK_PERIODE_TOM -> historikkOpplysningType.getNavn(); // TODO: ingen feltid frontend?
            default -> throw new IllegalStateException("Unexpected value: " + historikkOpplysningType);
        };
        return tekst.replace("{antallBarn}", opplysning.getTilVerdi());
    }

    private static String finnEndretFeltVerdi(HistorikkinnslagFelt felt, Object verdi) {
        if (verdi == null) {
            return null;
        } else if (isBoolean(String.valueOf(verdi))) {
            return konverterBoolean(String.valueOf(verdi));
        } else if (felt.getKlTilVerdi() != null) { //TODO Thao: Test på denne. Ser ikke helt logisk ut. Koden kommer kanskje aldri hit.
            try {
                return kodeverdiTilStrengEndretFeltTilverdi(felt.getTilVerdiKode(), String.valueOf(verdi)); // TODO Thao: Skal det kun sjekkes for getTilVerdiKode?
            } catch (IllegalStateException e) {
                return String.format("EndretFeltTypeTilVerdiKode %s finnes ikke-LEGG DET INN", felt.getTilVerdiKode());
            }
        }
        return String.valueOf(verdi);
    }

    private static boolean isBoolean(String str) {
        return "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str);
    }

    // TODO Thao: Gjør om denne til felles klasse som kan brukes av flere maltype. Flytt denne til en util klasse
    private static String fraEndretFelt(HistorikkinnslagFelt felt) {
        var endretFeltNavn = HistorikkEndretFeltType.fraKode(felt.getNavn());

        var feltNavn = kodeverdiTilStreng(endretFeltNavn, felt.getNavnVerdi());

        String tilVerdi = konverterBoolean(felt.getTilVerdi());
        if (felt.getTilVerdi() != null && tilVerdi == null) {
            tilVerdi = kodeverdiTilStrengEndretFeltTilverdi(felt.getTilVerdiKode(), felt.getTilVerdi());
        }

        if (felt.getFraVerdi() == null || endretFeltNavn.equals(HistorikkEndretFeltType.FORDELING_FOR_NY_ANDEL)) {
            return String.format("<b>%s</b> er satt til <b>%s</b>.", feltNavn, tilVerdi);
        }

        String fraVerdi = konverterBoolean(felt.getFraVerdi());
        if (fraVerdi == null) {
            fraVerdi = kodeverdiTilStrengEndretFeltTilverdi(felt.getFraVerdiKode(), felt.getFraVerdi());
        }

        return String.format("<b>%s</b> endret fra %s til <b>%s</b>", feltNavn, fraVerdi, tilVerdi);

    }

    private static String konverterBoolean(String verdi) {
        if ("true".equalsIgnoreCase(verdi)) {
            return "Ja";
        } else if ("false".equalsIgnoreCase(verdi)) {
            return "Nei";
        } else {
            return null;
        }
    }

    private static String kodeverdiTilStrengEndretFeltTilverdi(String verdiKode, String verdi) {
        if (verdiKode == null) {
            return verdi;
        }

        return FeltType.getByKey(verdiKode).getText();
    }

    private static String kodeverdiTilStreng(HistorikkEndretFeltType endretFeltNavn, String verdi) {
        var tekstFrontend = FeltNavnType.getByKey(endretFeltNavn.getKode()).getText();

        if (tekstFrontend.contains("{value}")) {
            if (verdi == null) {
                LOG.info("historikkv2 manglende value - {} {}", endretFeltNavn, tekstFrontend);
            } else {
                return tekstFrontend.replace("{value}", verdi);
            }
        }
        return tekstFrontend;
    }

    private static String fraTema(HistorikkinnslagFelt tema) {
        var type = HistorikkEndretFeltType.fraKode(tema.getNavn());
        var tekst = switch (type) {
            case AKTIVITET -> "<b>Det er lagt til ny aktivitet:</b> <br/><b>{value}</b>"; // Finnes ikke frontend
            case FORDELING_FOR_NY_ANDEL -> "<b>Det er lagt til ny aktivitet:</b> <br/><b>{value}</b>: Fordeling for ";
            case FORDELING_FOR_ANDEL -> "Fordeling for <b>{value}</b>:";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
        return String.format(tekst, tema.getNavnVerdi());
    }

    private static String fraSøknadsperiode(HistorikkinnslagFelt søknadsperiode) {
        var type = HistorikkAvklartSoeknadsperiodeType.fraKode(søknadsperiode.getNavn());

        var tekst = switch (type) {
            case GRADERING -> "<b>Uttak: gradering</b> <br/>%s<br/>%s";
            case UTSETTELSE_ARBEID -> "<b>Utsettelse: Arbeid</b> <br/>%s";
            case UTSETTELSE_FERIE -> "<b>Utsettelse: Ferie</b> <br/>%s";
            case UTSETTELSE_SYKDOM -> "<b>Utsettelse: Sykdom/skade</b> <br/>%s";
            case UTSETTELSE_HV -> "<b>Utsettelse: Heimevernet</b> <br/>%s";
            case UTSETTELSE_TILTAK_I_REGI_AV_NAV -> "<b>Utsettelse: Tiltak i regi av NAV</b> <br/>%s";
            case UTSETTELSE_INSTITUSJON_SØKER -> "<b>Utsettelse: Innleggelse av forelder</b> <br/>%s";
            case UTSETTELSE_INSTITUSJON_BARN -> "<b>Utsettelse: Innleggelse av barn</b> <br/>%s";
            case NY_SOEKNADSPERIODE -> "<b>Ny periode er lagt til</b> <br/>%s";
            case SLETTET_SOEKNASPERIODE -> "<b>Perioden er slettet</b> <br/>%s";
            case OVERFOERING_ALENEOMSORG -> "<b>Overføring: søker har aleneomsorg</b> <br/>%s";
            case OVERFOERING_SKYDOM -> "<b>Overføring: sykdom/skade</b> <br/>%s";
            case OVERFOERING_INNLEGGELSE -> "<b>Overføring: innleggelse</b> <br/>%s";
            case OVERFOERING_IKKE_RETT -> "<b>Overføring: annen forelder har ikke rett</b> <br/>%s";
            case UTTAK -> "<b>Uttak</b> <br/>%s";
            case OPPHOLD -> "<b>Opphold: annen foreldres uttak</b> <br/>%s";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
        return type.equals(HistorikkAvklartSoeknadsperiodeType.GRADERING) ? String.format(tekst, søknadsperiode.getNavnVerdi(),
            søknadsperiode.getTilVerdi()) : String.format(tekst, søknadsperiode.getTilVerdi());
    }

    private static String tilGjeldendeFraInnslag(HistorikkinnslagFelt gjeldendeFra, HistorikkinnslagDel del) {
        // Fra DB: Hverken navn, navnverdi, tilVerdi er null (FPSAK)
        var historikkEndretFeltType = HistorikkEndretFeltType.fraKode(gjeldendeFra.getNavn());

        var endretFeltTekst = switch (historikkEndretFeltType) {
            case NY_AKTIVITET -> "Det er lagt til ny aktivitet for <b>%s</b>";
            case NY_FORDELING -> "Ny fordeling <b>%s</b>";
            default -> throw new IllegalArgumentException();
        };
        // Historikk.Template.5.VerdiGjeldendeFra
        var verditekst = String.format(" gjeldende fra <b>%s</b>:", gjeldendeFra.getTilVerdi());

        return String.format(endretFeltTekst, gjeldendeFra.getNavnVerdi()) + verditekst + (del.getEndredeFelt()
            .isEmpty() ? "Ingen endring av vurdering" : "");
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
            case ANKE_AVVIS, ANKE_OMGJOER, ANKE_OPPHEVE_OG_HJEMSENDE, ANKE_HJEMSENDE, ANKE_STADFESTET_VEDTAK, ANKE_DELVIS_OMGJOERING_TIL_GUNST,
                 ANKE_TIL_UGUNST, ANKE_TIL_GUNST -> historikkResultatType.getNavn(); // Ikke i frontend
            default -> historikkResultatType.getNavn();
        };
    }


    private static HistorikkinnslagDtoV2 fraMaltype2(Historikkinnslag h, UUID behandlingUUID) {

        return null; // TODO:
    }


    private static HistorikkinnslagDtoV2 fraMaltype1(Historikkinnslag innslag, UUID behandlingUUID) {
        var historikkinnslagDel = innslag.getHistorikkinnslagDeler().getFirst();
        var skjermlenke = historikkinnslagDel.getSkjermlenke().map(SkjermlenkeType::fraKode).orElse(null);
        var lenker = tilDto(innslag.getDokumentLinker());
        var begrunnelsetekst = begrunnelseFraDel(historikkinnslagDel).map(List::of);

        var body = begrunnelsetekst.orElse(List.of());
        return new HistorikkinnslagDtoV2(behandlingUUID, HistorikkinnslagDtoV2.HistorikkAktørDto.fra(innslag.getAktør(), innslag.getOpprettetAv()),
            skjermlenke, innslag.getOpprettetTidspunkt(), lenker, innslag.getType().getNavn(), body);
    }

    private static Optional<String> begrunnelseFraDel(HistorikkinnslagDel historikkinnslagDel) {
        return historikkinnslagDel.getBegrunnelseFelt()
            .flatMap(HistorikkV2Adapter::finnÅrsakKodeListe)
            .map(Kodeverdi::getNavn)
            .or(historikkinnslagDel::getBegrunnelse);
    }

    private static List<HistorikkInnslagDokumentLinkDto> tilDto(List<HistorikkinnslagDokumentLink> dokumentLinker) {
        if (dokumentLinker == null) {
            return List.of();
        }
        return dokumentLinker.stream().map(d -> tilDto(d, null, null)) // TODO: husks aktiv journalpost og full URI
            .toList();
    }

    private static HistorikkInnslagDokumentLinkDto tilDto(HistorikkinnslagDokumentLink lenke,
                                                          List<JournalpostId> journalPosterForSak,
                                                          URI dokumentPath) {
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(lenke.getLinkTekst());
        // dto.setUtgått(aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak));
        dto.setUtgått(true); //TODO
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        if (lenke.getJournalpostId().getVerdi() != null && lenke.getDokumentId() != null && dokumentPath != null) {
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
