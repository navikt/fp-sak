package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType.TILBAKEKREVING_VIDEREBEHANDLING;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkDtoFellesMapper.konverterTilLinjerMedLinjeskift;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkDtoFellesMapper.tilHistorikkInnslagDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTotrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public class HistorikkV2Adapter {

    private static final Logger LOG = LoggerFactory.getLogger(HistorikkV2Adapter.class);

    public static HistorikkinnslagDtoV2 map(Historikkinnslag h, UUID behandlingUUID, List<JournalpostId> journalPosterForSak, URI dokumentPath) {
        return switch (h.getType()) {
            case BEH_GJEN, KØET_BEH_GJEN, BEH_MAN_GJEN, BEH_STARTET, BEH_STARTET_PÅ_NYTT,
                 // BEH_STARTET_FORFRA,  finnes ikke lenger? fptilbake?
                 VEDLEGG_MOTTATT, BREV_SENT, BREV_BESTILT, MIN_SIDE_ARBEIDSGIVER, REVURD_OPPR, REGISTRER_PAPIRSØK, MANGELFULL_SØKNAD, INNSYN_OPPR,
                 VRS_REV_IKKE_SNDT, NYE_REGOPPLYSNINGER, BEH_AVBRUTT_VUR, BEH_OPPDATERT_NYE_OPPL, SPOLT_TILBAKE,
                 // TILBAKEKREVING_OPPR,  fptilbake
                 MIGRERT_FRA_INFOTRYGD, MIGRERT_FRA_INFOTRYGD_FJERNET, ANKEBEH_STARTET, KLAGEBEH_STARTET, ENDRET_DEKNINGSGRAD, OPPGAVE_VEDTAK ->
                fraMaltype1(h, behandlingUUID, journalPosterForSak, dokumentPath);
            case FORSLAG_VEDTAK,
                 FORSLAG_VEDTAK_UTEN_TOTRINN,
                 VEDTAK_FATTET,
                 // VEDTAK_FATTET_AUTOMATISK, que? fptilbake?
                 UENDRET_UTFALL,
                 REGISTRER_OM_VERGE
                -> fraMaltype2(h, behandlingUUID);
            case SAK_RETUR -> fraMaltype3(h, behandlingUUID);
            case AVBRUTT_BEH,
                BEH_KØET,
                BEH_VENT,
                IVERKSETTELSE_VENT,
                FJERNET_VERGE -> fraMalType4(h, behandlingUUID);
            case SAK_GODKJENT, // Inneholder også aksjonspunkt som ikke vises i frontend
                 FAKTA_ENDRET, KLAGE_BEH_NK, KLAGE_BEH_NFP, BYTT_ENHET, UTTAK, TERMINBEKREFTELSE_UGYLDIG, ANKE_BEH ->
                fraMalType5(h, behandlingUUID, journalPosterForSak, dokumentPath);
            case NY_INFO_FRA_TPS
                 //NY_GRUNNLAG_MOTTATT fptilbake
                -> fraMalType6(h, behandlingUUID);
            case OVERSTYRT-> fraMalType7(h, behandlingUUID, journalPosterForSak, dokumentPath);
            case OPPTJENING -> throw new IllegalStateException(String.format("Kode: %s har ingen maltype", h.getType())); // Ingen historikkinnslag for denne typen i DB!
            case OVST_UTTAK_SPLITT, FASTSATT_UTTAK_SPLITT, TILBAKEKREVING_VIDEREBEHANDLING -> fraMalType9(h, behandlingUUID);
            case OVST_UTTAK, FASTSATT_UTTAK -> fraMaltype10(h, behandlingUUID, journalPosterForSak, dokumentPath);
            case AVKLART_AKTIVITETSKRAV -> fraMalTypeAktivitetskrav(h, behandlingUUID);
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + h.getType());
        };
    }

    private static HistorikkinnslagDtoV2 fraMaltype1(Historikkinnslag innslag, UUID behandlingUUID, List<JournalpostId> journalPosterForSak, URI dokumentPath) {
        var del = innslag.getHistorikkinnslagDeler().getFirst();
        var begrunnelse = begrunnelseFraDel(del);

        if (begrunnelse.isPresent() && Objects.equals(innslag.getType().getNavn(), begrunnelse.get())) {
            // ANKEBEH_STARTET, KLAGEBEH_STARTET, INNSYN_OPPR, BEH_STARTET_PÅ_NYTT Begrunnelse og tittel er identisk for disse. Fører til duplikate innslag
            // Kanskje flere? Sjekker derfor alle typer
            return tilHistorikkInnslagDto(innslag, behandlingUUID, tilDokumentlenker(innslag.getDokumentLinker(), journalPosterForSak, dokumentPath), List.of());
        }
        var tekster = begrunnelseFraDel(del).map(List::of).orElseGet(List::of);
        return tilHistorikkInnslagDto(innslag, behandlingUUID, tilDokumentlenker(innslag.getDokumentLinker(), journalPosterForSak, dokumentPath), konverterTilLinjerMedLinjeskift(tekster));
    }

    private static HistorikkinnslagDtoV2 fraMaltype2(Historikkinnslag h, UUID behandlingUUID) {
        var del = h.getHistorikkinnslagDeler().getFirst();
        var hendelse = del.getHendelse().map(HistorikkDtoFellesMapper::fraHendelseFelt).orElseThrow();
        var tekst = del.getResultatFelt()
            .map(s -> String.format("%s: %s", hendelse, fraHistorikkResultat(s)))
            .orElse(hendelse);
        return tilHistorikkInnslagDto(h, behandlingUUID, konverterTilLinjerMedLinjeskift(List.of(tekst)));
    }

    private static HistorikkinnslagDtoV2 fraMaltype3(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<HistorikkinnslagDtoV2.Linje>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var aksjonspunkt = del.getTotrinnsvurderinger().stream()
                .map(HistorikkV2Adapter::fraAksjonspunktFelt)
                .flatMap(List::stream)
                .toList();
            tekster.addAll(konverterTilLinjerMedLinjeskift(aksjonspunkt));
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tekster);
    }

    private static HistorikkinnslagDtoV2 fraMalType4(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<HistorikkinnslagDtoV2.Linje>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var årsakTekst = del.getAarsakFelt().stream()
                .flatMap(felt -> finnÅrsakKodeListe(felt).stream())
                .map(Kodeverdi::getNavn)
                .toList();
            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            tekster.addAll(konverterTilLinjerMedLinjeskift(årsakTekst, begrunnelsetekst));
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tekster);
    }

    private static HistorikkinnslagDtoV2 fraMalType5(Historikkinnslag h, UUID behandlingUUID,
                                                     List<JournalpostId> journalPosterForSak,
                                                     URI dokumentPath) {
        var tekster = new ArrayList<HistorikkinnslagDtoV2.Linje>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var resultatTekst = del.getResultatFelt().stream()
                .map(HistorikkV2Adapter::fraHistorikkResultat)
                .toList();
            var gjeldendeFraInnslag = del.getGjeldendeFraFelt().stream()
                .map(felt -> tilGjeldendeFraInnslag(felt, del))
                .toList();
            var søknadsperiode = del.getAvklartSoeknadsperiode().stream()
                .map(HistorikkV2Adapter::fraSøknadsperiode)
                .flatMap(List::stream)
                .toList();
            var tema = del.getTema().stream()
                .map(HistorikkV2Adapter::fraTema)
                .flatMap(List::stream)
                .toList();
            var endretFelter = del.getEndredeFelt().stream()
                .flatMap(felt -> fraEndretFelt(felt).stream())
                .toList();
            var opplysninger = del.getOpplysninger().stream()
                .map(HistorikkV2Adapter::fraOpplysning)
                .toList();
            var årsaktekst = del.getAarsakFelt().stream()
                .flatMap(felt -> finnÅrsakKodeListe(felt).stream())
                .map(Kodeverdi::getNavn)
                .toList();
            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            tekster.addAll(konverterTilLinjerMedLinjeskift(resultatTekst, gjeldendeFraInnslag, søknadsperiode, tema, endretFelter,
                opplysninger, årsaktekst, begrunnelsetekst));
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tilDokumentlenker(h.getDokumentLinker(), journalPosterForSak, dokumentPath), tekster);
    }

    private static HistorikkinnslagDtoV2 fraMalType6(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<HistorikkinnslagDtoV2.Linje>();
        for (var del : h.getHistorikkinnslagDeler()) {
            var opplysninger = del.getOpplysninger().stream()
                .map(HistorikkV2Adapter::fraOpplysning)
                .toList();

            tekster.addAll(konverterTilLinjerMedLinjeskift(opplysninger));
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tekster);

    }

    private static HistorikkinnslagDtoV2 fraMalType7(Historikkinnslag h, UUID behandlingUUID,
                                                     List<JournalpostId> journalPosterForSak,
                                                     URI dokumentPath) {
        var tekster = new ArrayList<HistorikkinnslagDtoV2.Linje>();
        for (var del : h.getHistorikkinnslagDeler()) {
            // HENDELSE finnes ikke i DB for denne maltypen
            var resultatTekst = del.getResultatFelt().stream()
                .map(HistorikkV2Adapter::fraHistorikkResultat)
                .toList();
            var endretFelter = del.getEndredeFelt().stream()
                .filter(felt -> felt.getFraVerdi() != null)
                .flatMap(felt -> fraEndretFelt(felt).stream())
                .toList();
            // OPPLYSNINGER finnes ikke i DB for denne maltypen
            var tema = del.getTema().stream() // Vises bare getNavnVerdi... gjør forbedring her
                .map(HistorikkV2Adapter::fraTema)
                .flatMap(List::stream)
                .toList();
            // AARSAK finnes ikke i DB for denne maltypen
            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            tekster.addAll(konverterTilLinjerMedLinjeskift(resultatTekst, endretFelter, tema, begrunnelsetekst));
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tilDokumentlenker(h.getDokumentLinker(), journalPosterForSak, dokumentPath), tekster);
    }



    private static HistorikkinnslagDtoV2 fraMalType9(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<HistorikkinnslagDtoV2.Linje>();
        for (var del : h.getHistorikkinnslagDeler()) {
            var endretFeltTekst = h.getType().equals(TILBAKEKREVING_VIDEREBEHANDLING)
                ? fraEndretFeltMalType9Tilbakekr(del)
                : fraEndretFeltMalType9(h, del);
            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            tekster.addAll(konverterTilLinjerMedLinjeskift(endretFeltTekst, begrunnelsetekst));
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tekster);
    }

    private static HistorikkinnslagDtoV2 fraMaltype10(Historikkinnslag h, UUID behandlingUUID,
                                                      List<JournalpostId> journalPosterForSak,
                                                      URI dokumentPath) {
        var tekster = new ArrayList<HistorikkinnslagDtoV2.Linje>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var periodeFom = periodeFraDel(del, "UTTAK_PERIODE_FOM");
            var periodeTom = periodeFraDel(del, "UTTAK_PERIODE_TOM");

            var opplysningTekst = "";
            if (h.getType().equals(HistorikkinnslagType.OVST_UTTAK)) {
                opplysningTekst = String.format("__Overstyrt vurdering__ av perioden %s - %s.", periodeFom, periodeTom);

            }
            if (h.getType().equals(HistorikkinnslagType.FASTSATT_UTTAK)) {
                opplysningTekst = String.format("__Manuell vurdering__ av perioden %s - %s.", periodeFom, periodeTom);
            }

            var endretFelter = del.getEndredeFelt().stream()
                .sorted(Comparator.comparing(HistorikkV2Adapter::sortering))
                .flatMap(felt -> fraEndretFelt(felt).stream())
                .toList();

            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            tekster.addAll(konverterTilLinjerMedLinjeskift(Collections.singletonList(opplysningTekst), endretFelter, begrunnelsetekst));

        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tilDokumentlenker(h.getDokumentLinker(), journalPosterForSak, dokumentPath), tekster);
    }

    private static String periodeFraDel(HistorikkinnslagDel del, String feltNavn) {
        return del.getOpplysninger()
            .stream()
            .filter(o -> feltNavn.equals(o.getNavn()))
            .map(HistorikkinnslagFelt::getTilVerdi)
            .findFirst()
            .orElse("");
    }

    // Frontend legger UTTAK_PERIODE_RESULTAT_TYPE sist, mens resten følger sekvensnummeret
    private static Integer sortering(HistorikkinnslagFelt felt) {
        return switch (FeltNavnType.getByKey(felt.getNavn())) {
            case UTTAK_PERIODE_RESULTAT_TYPE -> Integer.MAX_VALUE;
            default -> felt.getSekvensNr();
        };
    }

    private static HistorikkinnslagDtoV2 fraMalTypeAktivitetskrav(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<HistorikkinnslagDtoV2.Linje>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var endretFelter = byggEndretFeltTekstForAktivitetskravMal(del);
            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            tekster.addAll(konverterTilLinjerMedLinjeskift(Collections.singletonList(endretFelter), begrunnelsetekst));
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tekster);
    }

    private static List<String> fraEndretFeltMalType9Tilbakekr(HistorikkinnslagDel del) {
        return del.getEndredeFelt().stream()
            .filter(felt -> !TilbakekrevingVidereBehandling.INNTREKK.getKode().equals(felt.getTilVerdi()))
            .flatMap(felt -> fraEndretFelt(felt).stream())
            .toList();
    }

    private static List<String> fraEndretFeltMalType9(Historikkinnslag h, HistorikkinnslagDel del) {
        var opprinneligPeriode = del.getEndredeFelt().getFirst().getFraVerdi();
        var numberOfPeriods = String.valueOf(del.getEndredeFelt().size());
        var splitPeriods = tekstRepresentasjonAvListe(del.getEndredeFelt());
        var tekst =  switch (h.getType()) {
            case OVST_UTTAK_SPLITT -> "__Overstyrt vurdering__ av perioden {opprinneligPeriode}.";
            case FASTSATT_UTTAK_SPLITT -> "__Manuell vurdering__ av perioden {opprinneligPeriode}.";
            default -> throw new IllegalStateException("Ikke støttet type" + h);
        };

        return List.of(
            tekst.replace("{opprinneligPeriode}", opprinneligPeriode),
            "__Perioden__ er delt i {numberOfPeriods} og satt til __{splitPeriods}__"
                .replace("{numberOfPeriods}", numberOfPeriods)
                .replace("{splitPeriods}", splitPeriods)
        );
    }

    private static String tekstRepresentasjonAvListe(List<HistorikkinnslagFelt> endretFelt) {
        StringBuilder tekst = new StringBuilder();
        var size = endretFelt.size();
        for (int i = 0; i < size; i++) {
            if (i == size - 1) {
                tekst.append("og ").append(endretFelt.get(i).getTilVerdi());
            } else if (i == size - 2) {
                tekst.append(endretFelt.get(i).getTilVerdi()).append(" ");
            } else {
                tekst.append(endretFelt.get(i).getTilVerdi()).append(", ");
            }
        }
        return tekst.toString();

    }

    private static String byggEndretFeltTekstForAktivitetskravMal(HistorikkinnslagDel del) {
        var felt = del.getEndredeFelt().stream()
            .filter(e -> FeltNavnType.AKTIVITETSKRAV_AVKLARING.getKey().equals(e.getKlTilVerdi()))
            .findFirst()
            .orElseThrow();

        var tilVerdiNavn = FeltVerdiType.valueOf(felt.getTilVerdiKode()).name();
        var periodeFom = periodeFraDel(del, "UTTAK_PERIODE_FOM");
        var periodeTom = periodeFraDel(del, "UTTAK_PERIODE_TOM");


        if (felt.getFraVerdi() == null) {
            return String.format("Perioden __%s - %s__ er avklart til __%s__", periodeFom, periodeTom, tilVerdiNavn);
        } else {
            var fraVerdi = FeltVerdiType.valueOf(felt.getFraVerdiKode()).getText();
            return String.format("Perioden __%s - %s__ er endret fra %s til __%s__", periodeFom, periodeTom, fraVerdi, tilVerdiNavn);
        }
    }


    private static String fraOpplysning(HistorikkinnslagFelt opplysning) {
        return switch (opplysning.getNavn()) {
            case "ANTALL_BARN" -> "__Antall barn__ som brukes i behandlingen: __{antallBarn}__".replace("{antallBarn}", opplysning.getTilVerdi()); // Brukes bare av maltype 5
            case "TPS_ANTALL_BARN" -> "Antall barn {verdi}".replace("{verdi}", opplysning.getTilVerdi());  // Brukes av maltype 6
            case "FODSELSDATO" -> "Når ble barnet født? {verdi}".replace("{verdi}", opplysning.getTilVerdi()); // Brukes av maltype 6
            default -> throw new IllegalStateException("Unexpected value: " + opplysning.getNavn());
        };
    }

    public static Optional<String> fraEndretFelt(HistorikkinnslagFelt felt) {
        var feltNavnType = FeltNavnType.getByKey(felt.getNavn());
        var navn = kodeverdiTilStreng(feltNavnType, felt.getNavnVerdi());
        var tilVerdi = endretFeltVerdi(feltNavnType, felt.getTilVerdi(), felt.getTilVerdiKode());
        var fraVerdi = endretFeltVerdi(feltNavnType, felt.getFraVerdi(), felt.getFraVerdiKode());
        if (Objects.equals(getFraVerdi(fraVerdi), tilVerdi)) {
            return Optional.empty();
        }

        if (fraVerdi == null) {
            return Optional.of(String.format("__%s__ er satt til __%s__.", navn, tilVerdi));
        }
        if (tilVerdi == null) {
            return Optional.of(String.format("__%s %s__ er fjernet", navn, fraVerdi));
        }
        return Optional.of(String.format("__%s__ er endret fra %s til __%s__", navn, fraVerdi, tilVerdi));
    }

    private static String getFraVerdi(String fraVerdi) {
        return fraVerdi;
    }

    private static String endretFeltVerdi(FeltNavnType feltNavnType, String verdi, String verdiKode) {
        if (verdiKode != null && !verdiKode.equals("-")) {
            return FeltVerdiType.getByKey(verdiKode).getText();
        }
        if (verdi != null) {
            if (Set.of(FeltNavnType.UTTAK_PROSENT_UTBETALING, FeltNavnType.STILLINGSPROSENT).contains(feltNavnType)) {
                return String.format("%s%%", verdi);
            }
            return konverterBoolean(verdi);
        }
        return null;
    }

    private static String konverterBoolean(String verdi) {
        if ("true".equalsIgnoreCase(verdi)) {
            return "Ja";
        }
        if ("false".equalsIgnoreCase(verdi)) {
            return "Nei";
        }
        return verdi;
    }

    private static String kodeverdiTilStreng(FeltNavnType feltNavnType, String navnVerdi) {
        var tekstFrontend = feltNavnType.getText();
        if (tekstFrontend.contains("{value}")) {
            if (navnVerdi == null) {
                LOG.info("historikkv2 manglende value - {} {}", feltNavnType.getKey(), tekstFrontend);
            } else {
                return tekstFrontend.replace("{value}", navnVerdi);
            }
        }
        return tekstFrontend;
    }

    private static List<String> fraTema(HistorikkinnslagFelt tema) {
        var type = FeltNavnType.getByKey(tema.getNavn());
        var tekst = switch (type) {
            case AKTIVITET -> "__Det er lagt til ny aktivitet:__"; // Finnes ikke frontend
            case FORDELING_FOR_NY_ANDEL -> "__Det er lagt til ny aktivitet:__";
            case FORDELING_FOR_ANDEL -> "Fordeling for __{value}__:";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };

        if (type.equals(FeltNavnType.FORDELING_FOR_ANDEL)) {
            return List.of(tekst.replace("{value}", tema.getNavnVerdi()));
        } else {
            return List.of(
                tekst,
                String.format("__%s__", tema.getNavnVerdi())
            );
        }
    }

    private static List<String> fraSøknadsperiode(HistorikkinnslagFelt søknadsperiode) {
        var tekst = switch (søknadsperiode.getNavn()) {
            case "GRADERING" -> "__Gradering på grunn av arbeid__";
            case "UTSETTELSE_ARBEID" -> "__Utsettelse: Arbeid__";
            case "UTSETTELSE_FERIE" -> "__Utsettelse: Ferie__";
            case "UTSETTELSE_SKYDOM" -> "__Utsettelse: Sykdom/skade__";
            case "UTSETTELSE_HV" -> "__Utsettelse: Heimevernet__";
            case "UTSETTELSE_TILTAK_I_REGI_AV_NAV" -> "__Utsettelse: Tiltak i regi av NAV__";
            case "UTSETTELSE_INSTITUSJON_SØKER" -> "__Utsettelse: Innleggelse av forelder__";
            case "UTSETTELSE_INSTITUSJON_BARN" -> "__Utsettelse: Innleggelse av barn__";
            case "NY_SOEKNADSPERIODE" -> "__Ny periode er lagt til__";
            case "SLETTET_SOEKNASPERIODE" -> "__Perioden er slettet__";
            case "OVERFOERING_ALENEOMSORG" -> "__Overføring: søker har aleneomsorg__";
            case "OVERFOERING_SKYDOM" -> "__Overføring: sykdom/skade__";
            case "OVERFOERING_INNLEGGELSE" -> "__Overføring: innleggelse__";
            case "OVERFOERING_IKKE_RETT" -> "__Overføring: annen forelder har ikke rett__";
            case "UTTAK" -> "__Uttak__";
            case "OPPHOLD" -> "__Opphold: annen foreldres uttak__";
            default -> throw new IllegalStateException("Unexpected value: " + søknadsperiode.getNavn());
        };

        if ("GRADERING".equals(søknadsperiode.getNavn()) && søknadsperiode.getNavnVerdi() != null) {
            return List.of(
                tekst,
                søknadsperiode.getNavnVerdi(),
                søknadsperiode.getTilVerdi()
            );
        } else {
            return List.of(
                tekst,
                søknadsperiode.getTilVerdi()
            );
        }
    }

    private static String tilGjeldendeFraInnslag(HistorikkinnslagFelt gjeldendeFra, HistorikkinnslagDel del) {
        // Fra DB: Hverken navn, navnverdi, tilVerdi er null (FPSAK)
        var feltNavnType = FeltNavnType.getByKey(gjeldendeFra.getNavn());

        var endretFeltTekst = switch (feltNavnType) {
            case NY_AKTIVITET -> "Det er lagt til ny aktivitet for __%s__";
            case NY_FORDELING -> "Ny fordeling __%s__";
            default -> throw new IllegalArgumentException();
        };
        // Historikk.Template.5.VerdiGjeldendeFra
        var verditekst = String.format(" gjeldende fra __%s__:", gjeldendeFra.getTilVerdi());

        return String.format(endretFeltTekst, gjeldendeFra.getNavnVerdi()) + verditekst + (del.getEndredeFelt()
            .isEmpty() ? "Ingen endring av vurdering" : "");
    }


    private static String fraHistorikkResultat(HistorikkinnslagFelt resultat) {
        return resultat.getKlTilVerdi().equals(HistorikkResultatType.KODEVERK)
            ? HistorikkResultatType.valueOf(resultat.getTilVerdiKode()).getNavn()
            : VedtakResultatType.valueOf(resultat.getTilVerdiKode()).getNavn();
    }

    private static Optional<String> begrunnelseFraDel(HistorikkinnslagDel historikkinnslagDel) {
        return historikkinnslagDel.getBegrunnelseFelt()
            .flatMap(HistorikkV2Adapter::finnÅrsakKodeListe)
            .map(Kodeverdi::getNavn)
            .or(historikkinnslagDel::getBegrunnelse);
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
        /**
         * kl_til_verdi er satt til enten: VENT_AARSAK, BEHANDLING_RESULTAT_TYPE, KLAGE_MEDHOLD_AARSAK eller KLAGE_AVVIST_AARSAK
         */
        return Optional.ofNullable(switch (aarsak.getKlTilVerdi()) {
            case "VENT_AARSAK" -> Venteårsak.fraKode(aarsakVerdi);
            case "BEHANDLING_RESULTAT_TYPE" -> BehandlingResultatType.fraKode(aarsakVerdi);
            case "KLAGE_MEDHOLD_AARSAK" -> KlageMedholdÅrsak.kodeMap().get(aarsakVerdi);
            case "KLAGE_AVVIST_AARSAK" -> KlageAvvistÅrsak.kodeMap().get(aarsakVerdi);
            default -> throw new IllegalStateException("Har ikke støtte for HistorikkinnslagFelt#klTilVerdi=" + aarsak.getKlTilVerdi());
        });
    }

    private static List<HistorikkInnslagDokumentLinkDto> tilDokumentlenker(List<HistorikkinnslagDokumentLink> dokumentLinker,
                                                                           List<JournalpostId> journalPosterForSak,
                                                                           URI dokumentPath) {
        if (dokumentLinker == null) {
            return List.of();
        }
        return dokumentLinker.stream().map(d -> tilDokumentlenker(d, journalPosterForSak, dokumentPath)) //
            .toList();
    }

    private static HistorikkInnslagDokumentLinkDto tilDokumentlenker(HistorikkinnslagDokumentLink lenke,
                                                                     List<JournalpostId> journalPosterForSak,
                                                                     URI dokumentPath) {
        var erUtgått = aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak);
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(erUtgått ? String.format("%s (utgått)", lenke.getLinkTekst()) : lenke.getLinkTekst());
        dto.setUtgått(erUtgått);
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

    private static List<String> fraAksjonspunktFelt(HistorikkinnslagTotrinnsvurdering aksjonspunktFelt) {
        var aksjonspunktTekst = aksjonspunktFelt.getAksjonspunktDefinisjon().getNavn();
        if (aksjonspunktFelt.erGodkjent()) {
            return List.of(String.format("__%s er godkjent__", aksjonspunktTekst));
        } else {
            return List.of(
                String.format("__%s må vurderes på nytt__", aksjonspunktTekst),
                String.format("Kommentar: %s", aksjonspunktFelt.getBegrunnelse())
            );
        }
    }
}
