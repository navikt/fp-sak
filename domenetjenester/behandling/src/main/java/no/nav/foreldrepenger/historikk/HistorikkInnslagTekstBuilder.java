package no.nav.foreldrepenger.historikk;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBegrunnelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagMal;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTotrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

public class HistorikkInnslagTekstBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Kodeverdi mappinger støttet i historikk. */
    public static final Map<String, Map<String, ? extends Kodeverdi>> KODEVERK_KODEVERDI_MAP = Map.ofEntries(
            new SimpleEntry<>(Venteårsak.KODEVERK, Venteårsak.kodeMap()),
            new SimpleEntry<>(OppgaveÅrsak.KODEVERK, OppgaveÅrsak.kodeMap()),

            new SimpleEntry<>(BehandlingÅrsakType.KODEVERK, BehandlingÅrsakType.kodeMap()),
            new SimpleEntry<>(BehandlingResultatType.KODEVERK, BehandlingResultatType.kodeMap()),

            new SimpleEntry<>(HistorikkAvklartSoeknadsperiodeType.KODEVERK, HistorikkAvklartSoeknadsperiodeType.kodeMap()),
            new SimpleEntry<>(HistorikkBegrunnelseType.KODEVERK, HistorikkBegrunnelseType.kodeMap()),
            new SimpleEntry<>(HistorikkEndretFeltType.KODEVERK, HistorikkEndretFeltType.kodeMap()),
            new SimpleEntry<>(HistorikkEndretFeltVerdiType.KODEVERK, HistorikkEndretFeltVerdiType.kodeMap()),
            new SimpleEntry<>(HistorikkinnslagType.KODEVERK, HistorikkinnslagType.kodeMap()),
            new SimpleEntry<>(HistorikkOpplysningType.KODEVERK, HistorikkOpplysningType.kodeMap()),
            new SimpleEntry<>(HistorikkResultatType.KODEVERK, HistorikkResultatType.kodeMap()),

            new SimpleEntry<>(SkjermlenkeType.KODEVERK, SkjermlenkeType.kodeMap()),

            new SimpleEntry<>(VedtakResultatType.KODEVERK, VedtakResultatType.kodeMap()),
            new SimpleEntry<>(VilkårUtfallType.KODEVERK, VilkårUtfallType.kodeMap()),

            // ulike domenespesifikke kodeverk som tillates

            // Domene : Uttak
            new SimpleEntry<>(PeriodeResultatType.KODEVERK, PeriodeResultatType.kodeMap()),
            new SimpleEntry<>(PeriodeResultatÅrsak.KODEVERK, PeriodeResultatÅrsak.kodeMap()),
            new SimpleEntry<>(StønadskontoType.KODEVERK, StønadskontoType.kodeMap()),
            new SimpleEntry<>(GraderingAvslagÅrsak.KODEVERK, GraderingAvslagÅrsak.kodeMap()),

            // Domene : personopplysninger
            new SimpleEntry<>(VergeType.KODEVERK, VergeType.kodeMap()),
            new SimpleEntry<>(PersonstatusType.KODEVERK, PersonstatusType.kodeMap()),

            // Domene : Medlemskap
            new SimpleEntry<>(MedlemskapManuellVurderingType.KODEVERK, MedlemskapManuellVurderingType.kodeMap()),

            // Domene : arbeid og beregningsgrunnlag
            new SimpleEntry<>(Inntektskategori.KODEVERK, Inntektskategori.kodeMap()),
            new SimpleEntry<>(VurderArbeidsforholdHistorikkinnslag.KODEVERK, VurderArbeidsforholdHistorikkinnslag.kodeMap()),
            new SimpleEntry<>(ArbeidsforholdKomplettVurderingType.KODEVERK, ArbeidsforholdKomplettVurderingType.kodeMap()),

            // Domene : klage og anke
            new SimpleEntry<>(KlageMedholdÅrsak.KODEVERK, KlageMedholdÅrsak.kodeMap()),
            new SimpleEntry<>(KlageAvvistÅrsak.KODEVERK, KlageAvvistÅrsak.kodeMap()),

            // Domene : Tilbakekreving
            new SimpleEntry<>(TilbakekrevingVidereBehandling.KODEVERK, TilbakekrevingVidereBehandling.kodeMap()));

    private boolean begrunnelseEndret = false;
    private boolean gjeldendeFraSatt = false;

    private HistorikkinnslagDel.Builder historikkinnslagDelBuilder = HistorikkinnslagDel.builder();
    private List<HistorikkinnslagDel> historikkinnslagDeler = new ArrayList<>();
    private int antallEndredeFelter = 0;
    private int antallAksjonspunkter = 0;
    private int antallOpplysninger = 0;

    public List<HistorikkinnslagDel> getHistorikkinnslagDeler() {
        return historikkinnslagDeler;
    }

    public HistorikkInnslagTekstBuilder medHendelse(HistorikkinnslagType historikkInnslagsType) {
        return medHendelse(historikkInnslagsType, null);
    }

    public HistorikkInnslagTekstBuilder medHendelse(HistorikkinnslagType historikkinnslagType, Object verdi) {
        if (!HistorikkinnslagType.FAKTA_ENDRET.equals(historikkinnslagType)
                && !HistorikkinnslagType.OVERSTYRT.equals(historikkinnslagType)
                && !HistorikkinnslagType.OPPTJENING.equals(historikkinnslagType)) { // PKMANTIS-753 FPFEIL-805
            var verdiStr = formatString(verdi);
            HistorikkinnslagFelt.builder()
                    .medFeltType(HistorikkinnslagFeltType.HENDELSE)
                    .medNavn(validerKodeverdi(historikkinnslagType))
                    .medTilVerdi(verdiStr)
                    .build(historikkinnslagDelBuilder);
        }
        return this;
    }

    public boolean erSkjermlenkeSatt() {
        return getHistorikkinnslagDeler().stream()
                .anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
    }

    public HistorikkInnslagTekstBuilder medSkjermlenke(SkjermlenkeType skjermlenkeType) {
        if (!SkjermlenkeType.totrinnsSkjermlenke(skjermlenkeType)) {
            return this;
        }
        validerKodeverdi(skjermlenkeType);
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.SKJERMLENKE)
                .medTilVerdi(validerKodeverdi(skjermlenkeType))
                .build(historikkinnslagDelBuilder);
        return this;
    }

    private Kodeverdi validerKodeverdi(Kodeverdi kodeverdi) {
        // validerer all input til HistorikkinnslagFelt#medTilVerdi(Kodeverdi).
        // ikke helt ideelt å ha validering utenfor HistorikkinnslagFelt, men nødvendig
        // da Kodeverdi kan stamme fra andre plasser
        // Hvis det ikke valideres er det en mulighet for at det smeller i
        // HistorikkinnslagDto ved mapping til GUI.
        if (!KODEVERK_KODEVERDI_MAP.containsKey(kodeverdi.getKodeverk())) {
            throw new IllegalStateException("Har ikke støtte for kodeverk :" + kodeverdi.getKodeverk() + " for Kodeverdi " + kodeverdi);
        }
        return kodeverdi;
    }

    public HistorikkInnslagTekstBuilder medNavnOgGjeldendeFra(HistorikkEndretFeltType endretFelt, String navnVerdi, LocalDate gjeldendeFraDato) {
        if (gjeldendeFraDato != null) {
            gjeldendeFraSatt = true;
        }
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.GJELDENDE_FRA)
                .medNavn(validerKodeverdi(endretFelt))
                .medNavnVerdi(navnVerdi)
                .medTilVerdi(formatString(gjeldendeFraDato))
                .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medÅrsak(Venteårsak årsak) {
        return medÅrsakIntern(årsak);
    }

    public HistorikkInnslagTekstBuilder medÅrsak(BehandlingResultatType årsak) {
        return medÅrsakIntern(årsak);
    }

    private <K extends Kodeverdi> HistorikkInnslagTekstBuilder medÅrsakIntern(K årsak) {
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.AARSAK)
                .medTilVerdi(validerKodeverdi(årsak))
                .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medTema(HistorikkEndretFeltType endretFeltType, String verdi) {
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.ANGÅR_TEMA)
                .medNavn(validerKodeverdi(endretFeltType))
                .medNavnVerdi(verdi)
                .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medResultat(HistorikkResultatType resultat) {
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.RESULTAT)
                .medTilVerdi(validerKodeverdi(resultat))
                .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medResultat(VedtakResultatType resultat) {
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.RESULTAT)
                .medTilVerdi(validerKodeverdi(resultat))
                .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medBegrunnelse(LocalDateInterval begrunnelse) {
        return medBegrunnelse(formatString(begrunnelse), true);
    }

    public HistorikkInnslagTekstBuilder medBegrunnelse(LocalDate begrunnelse) {
        return medBegrunnelse(formatString(begrunnelse), true);
    }

    public HistorikkInnslagTekstBuilder medBegrunnelse(Kodeverdi begrunnelse) {
        return medBegrunnelse(begrunnelse, true);
    }

    public HistorikkInnslagTekstBuilder medBegrunnelse(String begrunnelse) {
        var begrunnelseStr = formatString(begrunnelse);
        return medBegrunnelse(begrunnelseStr, true);
    }

    public HistorikkInnslagTekstBuilder medBegrunnelse(String begrunnelse, boolean erBegrunnelseEndret) {
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.BEGRUNNELSE)
                .medTilVerdi(begrunnelse)
                .build(historikkinnslagDelBuilder);
        this.begrunnelseEndret = erBegrunnelseEndret;
        return this;
    }

    public <K extends Kodeverdi> HistorikkInnslagTekstBuilder medBegrunnelse(K begrunnelse, boolean erBegrunnelseEndret) {
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.BEGRUNNELSE)
                .medTilVerdi(validerKodeverdi(begrunnelse))
                .build(historikkinnslagDelBuilder);
        this.begrunnelseEndret = erBegrunnelseEndret;
        return this;
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, String navnVerdi, Integer fraVerdi,
            Integer tilVerdi) {
        if (Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        return medEndretFelt(historikkEndretFeltType, navnVerdi, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, String navnVerdi, Boolean fraVerdi,
            Boolean tilVerdi) {
        return medEndretFelt(historikkEndretFeltType, navnVerdi, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, String navnVerdi, String fraVerdi,
            String tilVerdi) {
        var fraVerdiStr = formatString(fraVerdi);
        var tilVerdiStr = formatString(tilVerdi);

        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
                .medNavn(validerKodeverdi(historikkEndretFeltType))
                .medNavnVerdi(navnVerdi)
                .medFraVerdi(fraVerdiStr)
                .medTilVerdi(tilVerdiStr)
                .medSekvensNr(getNesteEndredeFeltSekvensNr())
                .build(historikkinnslagDelBuilder);
        return this;
    }

    public <K extends Kodeverdi> HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, K fraVerdi, K tilVerdi) {
        if (Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
                .medNavn(validerKodeverdi(historikkEndretFeltType))
                .medFraVerdi(fraVerdi)
                .medTilVerdi(validerKodeverdi(tilVerdi))
                .medSekvensNr(getNesteEndredeFeltSekvensNr())
                .build(historikkinnslagDelBuilder);
        return this;
    }

    public <K extends Kodeverdi> HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, String navnVerdi,
            K fraVerdi,
            K tilVerdi) {
        if (Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
                .medNavn(validerKodeverdi(historikkEndretFeltType))
                .medNavnVerdi(navnVerdi)
                .medFraVerdi(fraVerdi)
                .medTilVerdi(validerKodeverdi(tilVerdi))
                .medSekvensNr(getNesteEndredeFeltSekvensNr())
                .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, Boolean fraVerdi, Boolean tilVerdi) {
        return medEndretFelt(historikkEndretFeltType, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType type, Number fraVerdi, Number tilVerdi) {
        return medEndretFelt(type, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType type, LocalDateInterval fraVerdi, LocalDateInterval tilVerdi) {
        return medEndretFelt(type, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType type, LocalDate fraVerdi, LocalDate tilVerdi) {
        return medEndretFelt(type, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType type, String navnVerdi, LocalDate fraVerdi, LocalDate tilVerdi) {
        return medEndretFelt(type, navnVerdi, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, String fraVerdi, String tilVerdi) {
        if (Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
                .medNavn(validerKodeverdi(historikkEndretFeltType))
                .medFraVerdi(fraVerdi)
                .medTilVerdi(tilVerdi)
                .medSekvensNr(getNesteEndredeFeltSekvensNr())
                .build(historikkinnslagDelBuilder);
        return this;
    }

    public <T> HistorikkInnslagTekstBuilder medOpplysning(HistorikkOpplysningType opplysningType, T verdi) {
        var tilVerdi = formatString(verdi);
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.OPPLYSNINGER)
                .medNavn(validerKodeverdi(opplysningType))
                .medTilVerdi(tilVerdi)
                .medSekvensNr(hentNesteOpplysningSekvensNr())
                .build(historikkinnslagDelBuilder);
        return this;
    }

    public static String formatString(Object verdi) {
        if (verdi == null) {
            return null;
        }
        if (verdi instanceof LocalDate localDate) {
            return formatDate(localDate);
        }
        if (verdi instanceof LocalDateInterval interval) {
            return formatDate(interval.getFomDato()) + " - " + formatDate(interval.getTomDato());
        }
        return verdi.toString();
    }

    private static String formatDate(LocalDate localDate) {
        return DATE_FORMATTER.format(localDate);
    }

    public HistorikkInnslagTekstBuilder medTotrinnsvurdering(Map<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>> vurdering,
            List<HistorikkinnslagTotrinnsvurdering> vurderingUtenVilkar) {
        var første = true;
        for (var totrinnsVurdering : vurderingUtenVilkar) {
            if (første) {
                første = false;
            } else {
                ferdigstillHistorikkinnslagDel();
            }
            leggTilTotrinnsvurdering(totrinnsVurdering);
        }

        var sortedList = vurdering.entrySet().stream()
                .sorted(getHistorikkDelComparator()).toList();

        for (var lenkeVurdering : sortedList) {
            if (første) {
                første = false;
            } else {
                ferdigstillHistorikkinnslagDel();
            }
            var skjermlenkeType = lenkeVurdering.getKey();
            var totrinnsVurderinger = lenkeVurdering.getValue();
            totrinnsVurderinger.sort(Comparator.comparing(HistorikkinnslagTotrinnsvurdering::getAksjonspunktSistEndret));
            medSkjermlenke(skjermlenkeType);
            totrinnsVurderinger.forEach(this::leggTilTotrinnsvurdering);
        }
        return this;
    }

    public int antallEndredeFelter() {
        return antallEndredeFelter;
    }

    /**
     * Returnerer om begrunnelse er endret.
     */
    public boolean getErBegrunnelseEndret() {
        return begrunnelseEndret;
    }

    /**
     * Returnerer om gjeldendeFra er satt.
     */
    public boolean getErGjeldendeFraSatt() {
        return gjeldendeFraSatt;
    }

    public HistorikkInnslagTekstBuilder ferdigstillHistorikkinnslagDel() {
        if (!historikkinnslagDelBuilder.harFelt()) {
            return this;
        }
        historikkinnslagDeler.add(historikkinnslagDelBuilder.build());
        historikkinnslagDelBuilder = HistorikkinnslagDel.builder();
        antallEndredeFelter = 0;
        antallAksjonspunkter = 0;
        antallOpplysninger = 0;
        begrunnelseEndret = false;
        return this;
    }

    public List<HistorikkinnslagDel> build(Historikkinnslag historikkinnslag) {
        ferdigstillHistorikkinnslagDel();
        verify(historikkinnslag.getType());
        historikkinnslag.setHistorikkinnslagDeler(historikkinnslagDeler);
        return historikkinnslagDeler;
    }

    private int getNesteEndredeFeltSekvensNr() {
        var neste = antallEndredeFelter;
        antallEndredeFelter++;
        return neste;
    }

    private int hentNesteOpplysningSekvensNr() {
        var sekvensNr = antallOpplysninger;
        antallOpplysninger++;
        return sekvensNr;
    }

    private Comparator<Map.Entry<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>>> getHistorikkDelComparator() {
        return (o1, o2) -> {
            var totrinnsvurderinger1 = o1.getValue();
            var totrinnsvurderinger2 = o2.getValue();
            totrinnsvurderinger1.sort(Comparator.comparing(HistorikkinnslagTotrinnsvurdering::getAksjonspunktSistEndret));
            totrinnsvurderinger2.sort(Comparator.comparing(HistorikkinnslagTotrinnsvurdering::getAksjonspunktSistEndret));
            var date1 = totrinnsvurderinger1.get(0).getAksjonspunktSistEndret();
            var date2 = totrinnsvurderinger2.get(0).getAksjonspunktSistEndret();
            if (date1 == null || date2 == null) {
                return -1;
            }
            return date1.isAfter(date2) ? 1 : -1;
        };
    }

    private HistorikkInnslagTekstBuilder leggTilTotrinnsvurdering(HistorikkinnslagTotrinnsvurdering totrinnsvurdering) {
        var sekvensNr = getNesteAksjonspunktSekvensNr();
        leggTilFelt(HistorikkinnslagFeltType.AKSJONSPUNKT_BEGRUNNELSE, totrinnsvurdering.getBegrunnelse(), sekvensNr);
        leggTilFelt(HistorikkinnslagFeltType.AKSJONSPUNKT_GODKJENT, totrinnsvurdering.erGodkjent(), sekvensNr);
        leggTilFelt(HistorikkinnslagFeltType.AKSJONSPUNKT_KODE, totrinnsvurdering.getAksjonspunktDefinisjon().getKode(), sekvensNr);
        return this;
    }

    private <T> void leggTilFelt(HistorikkinnslagFeltType feltType, T verdi, int sekvensNr) {
        HistorikkinnslagFelt.builder()
                .medFeltType(feltType)
                .medTilVerdi(verdi != null ? verdi.toString() : null)
                .medSekvensNr(sekvensNr)
                .build(historikkinnslagDelBuilder);
    }

    private int getNesteAksjonspunktSekvensNr() {
        var sekvensNr = antallAksjonspunkter;
        antallAksjonspunkter++;
        return sekvensNr;
    }

    /**
     * Sjekker at alle påkrevde felter for gitt historikkinnslagstype er angitt
     *
     * @param historikkinnslagType
     */
    private void verify(HistorikkinnslagType historikkinnslagType) {
        var verificationResults = new ArrayList<TekniskException>();
        historikkinnslagDeler.forEach(del -> {
            var exception = verify(historikkinnslagType, del);
            exception.ifPresent(verificationResults::add);
        });
        // kast feil dersom alle deler feiler valideringen
        if (verificationResults.size() == historikkinnslagDeler.size()) {
            throw verificationResults.get(0);
        }
    }

    private Optional<TekniskException> verify(HistorikkinnslagType historikkinnslagType, HistorikkinnslagDel historikkinnslagDel) {
        var type = historikkinnslagType.getMal();

        if (HistorikkinnslagMal.MAL_TYPE_1.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.HENDELSE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_2.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.HENDELSE, HistorikkinnslagFeltType.SKJERMLENKE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_3.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.HENDELSE, HistorikkinnslagFeltType.AKSJONSPUNKT_KODE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_4.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.HENDELSE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_5.equals(type) || HistorikkinnslagMal.MAL_TYPE_7.equals(type) || HistorikkinnslagMal.MAL_TYPE_8.equals(type)
                || HistorikkinnslagMal.MAL_TYPE_10.equals(type)) {
            return checkAtLeastOnePresent(type, historikkinnslagDel, HistorikkinnslagFeltType.SKJERMLENKE,
                    HistorikkinnslagFeltType.HENDELSE,
                    HistorikkinnslagFeltType.ENDRET_FELT,
                    HistorikkinnslagFeltType.BEGRUNNELSE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_11.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel,
                HistorikkinnslagFeltType.SKJERMLENKE,
                HistorikkinnslagFeltType.ENDRET_FELT,
                HistorikkinnslagFeltType.OPPLYSNINGER,
                HistorikkinnslagFeltType.BEGRUNNELSE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_6.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.OPPLYSNINGER);
        }
        if (HistorikkinnslagMal.MAL_TYPE_9.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.HENDELSE);
        }
        throw HistorikkInnsalgFeil.ukjentHistorikkinnslagType(type);
    }

    private Optional<TekniskException> checkFieldsPresent(String type, HistorikkinnslagDel del, HistorikkinnslagFeltType... fields) {
        var fieldList = Arrays.asList(fields);
        var harFelt = findFields(del, fieldList).collect(Collectors.toCollection(LinkedHashSet::new));

        // harFelt skal inneholde alle de samme feltene som fieldList
        if (harFelt.size() == fields.length) {
            return Optional.empty();
        }
        return Optional.of(HistorikkInnsalgFeil.manglerFeltForHistorikkInnslag(type, fieldList, harFelt));
    }

    private Optional<TekniskException> checkAtLeastOnePresent(String type, HistorikkinnslagDel del, HistorikkinnslagFeltType... fields) {
        var fieldList = Arrays.asList(fields);
        var opt = findFields(del, fieldList).findAny();

        if (opt.isPresent()) {
            return Optional.empty();
        }
        var feltKoder = fieldList.stream().map(HistorikkinnslagFeltType::getKode).toList();
        return Optional.of(HistorikkInnsalgFeil.manglerMinstEtFeltForHistorikkinnslag(type, feltKoder));
    }

    private Stream<HistorikkinnslagFeltType> findFields(HistorikkinnslagDel del, List<HistorikkinnslagFeltType> fieldList) {
        return del.getHistorikkinnslagFelt().stream().map(HistorikkinnslagFelt::getFeltType).filter(fieldList::contains);
    }

    /** Tar med felt selv om ikke verdi er endret. */
    public HistorikkInnslagTekstBuilder medEndretFeltBegrunnelse(HistorikkEndretFeltType historikkEndretFeltType, String fraVerdi, String tilVerdi) {
        if (!begrunnelseEndret && Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
                .medNavn(validerKodeverdi(historikkEndretFeltType))
                .medFraVerdi(fraVerdi)
                .medTilVerdi(tilVerdi)
                .medSekvensNr(getNesteEndredeFeltSekvensNr())
                .build(historikkinnslagDelBuilder);
        return this;
    }

    /*
     * https://confluence.adeo.no/display/MODNAV/OMR-13+SF4+Sakshistorikk+-+UX+og+
     * grafisk+design
     *
     * Fem design patterns:
     *
     * +----------------------------+ | Type 1 | | BEH_VENT | | BEH_GJEN | |
     * BEH_STARTET | | VEDLEGG_MOTTATT | | BREV_SENT | | REGISTRER_PAPIRSØK |
     * +----------------------------+ <tidspunkt> // <rolle> <id> <hendelse>
     * <OPTIONAL begrunnelsestekst>
     *
     *
     * +----------------------------+ | Type 2 | | FORSLAG_VEDTAK | | VEDTAK_FATTET
     * | | OVERSTYRT (hvis beslutter) | | UENDRET UTFALL |
     * +----------------------------+ <tidspunkt> // <rolle> <id> <hendelse>:
     * <resultat> <skjermlinke> <OPTIONAL totrinnskontroll>
     *
     *
     * +----------------------------+ | Type 3 | | SAK_RETUR |
     * +----------------------------+ <tidspunkt> // <rolle> <id> <hendelse>
     * <totrinnsvurdering> med <skjermlinke> til vilkåret og liste med
     * <aksjonspunkter>
     *
     *
     * +----------------------------+ | Type 4 | | AVBRUTT_BEH | | OVERSTYRT (hvis
     * saksbeh.) | +----------------------------+ <tidspunkt> // <rolle> <id>
     * <hendelse> <årsak> <begrunnelsestekst>
     *
     *
     * +----------------------------+ | Type 5 | | FAKTA_ENDRET |
     * +----------------------------+ <tidspunkt> // <rolle> <id> <skjermlinke>
     * <feltnavn> er endret <fra-verdi> til <til-verdi> <radiogruppe> er satt til
     * <verdi> <begrunnelsestekst>
     *
     */

}
