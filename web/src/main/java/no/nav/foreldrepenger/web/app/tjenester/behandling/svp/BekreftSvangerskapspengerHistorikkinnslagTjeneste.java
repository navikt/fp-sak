package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.svp.BekreftSvangerskapspengerOppdaterer.getFødselsdato;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.svp.BekreftSvangerskapspengerOppdaterer.getTermindato;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpAvklartOpphold;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class BekreftSvangerskapspengerHistorikkinnslagTjeneste {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    BekreftSvangerskapspengerHistorikkinnslagTjeneste() {
        //CDI
    }

    @Inject
    public BekreftSvangerskapspengerHistorikkinnslagTjeneste(ArbeidsgiverTjeneste arbeidsgiverTjeneste, HistorikkinnslagRepository historikkinnslagRepository) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void lagHistorikkinnslagVedEndring(BehandlingReferanse ref,
                                              BekreftSvangerskapspengerDto dto,
                                              FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag,
                                              List<SvpTilretteleggingEntitet> endredeTilrettelegginger,
                                              List<SvpTilretteleggingEntitet> eksisterendeTilretteleginger) {
        var linjer = endredeTilrettelegginger.stream()
            .filter(ny -> erTilretteleggingEndret(hentEksisterendeTilrettelegging(eksisterendeTilretteleginger, ny), ny))
            .map(ny -> oppprettHistorikkinnslagForTilretteleggingsperiode(hentEksisterendeTilrettelegging(eksisterendeTilretteleginger, ny), ny))
            .toList();
        var builder = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.PUNKT_FOR_SVP_INNGANG)
            .addLinje(fraTilEquals("Termindato", getTermindato(familieHendelseGrunnlag, ref), dto.getTermindato()))
            .addLinje(fraTilEquals("Fødselsdato", getFødselsdato(familieHendelseGrunnlag).orElse(null), dto.getFødselsdato()));
        linjer.forEach(t -> {
            builder.addLinje(HistorikkinnslagLinjeBuilder.LINJESKIFT);
            t.forEach(builder::addLinje);
            builder.addLinje(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        });
        builder.addLinje(dto.getBegrunnelse());
        historikkinnslagRepository.lagre(builder.build());
    }

    private List<HistorikkinnslagLinjeBuilder> oppprettHistorikkinnslagForTilretteleggingsperiode(SvpTilretteleggingEntitet eksisterendeTilrettelegging, SvpTilretteleggingEntitet nyTilrettelegging) {
        var endredeLinjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        endredeLinjer.add(new HistorikkinnslagLinjeBuilder().tekst(lagTekstForArbeidsgiver(nyTilrettelegging)));
        if (eksisterendeTilrettelegging.getSkalBrukes() != nyTilrettelegging.getSkalBrukes()) {
            endredeLinjer.add(fraTilEquals("Tilrettelegging skal brukes", eksisterendeTilrettelegging.getSkalBrukes(), nyTilrettelegging.getSkalBrukes()));
        }
        if (!Objects.equals(eksisterendeTilrettelegging.getBehovForTilretteleggingFom(), nyTilrettelegging.getBehovForTilretteleggingFom())) {
            endredeLinjer.add(fraTilEquals("Tilrettelegging er nødvendig fra og med", eksisterendeTilrettelegging.getBehovForTilretteleggingFom(), nyTilrettelegging.getBehovForTilretteleggingFom()));
        }

        //Tilrettelegging fra datoer
        var eksisterendeFoms = eksisterendeTilrettelegging.getTilretteleggingFOMListe();
        var nyeFoms = nyTilrettelegging.getTilretteleggingFOMListe();
        var fjernetListe = eksisterendeFoms.stream().filter(fom -> !nyeFoms.contains(fom));
        var lagtTilListe = nyeFoms.stream().filter(fom -> !eksisterendeFoms.contains(fom)).toList();

        //Oppholdsperioder
        var eksisterendeOpphold = eksisterendeTilrettelegging.getAvklarteOpphold();
        var nyeOpphold = nyTilrettelegging.getAvklarteOpphold();
        var fjernetOppholdListe = eksisterendeOpphold.stream()
            .filter(eksisterende -> nyeOpphold.stream().noneMatch(nyttOpphold -> erLikeUtenKilde(nyttOpphold, eksisterende)))
            .toList();
        var lagtTilOppholdListe = nyeOpphold.stream()
            .filter(nyttOpphold -> eksisterendeOpphold.stream().noneMatch(eksisterende -> erLikeUtenKilde(eksisterende, nyttOpphold)))
            .toList();

        fjernetListe.forEach(fomFjernet -> endredeLinjer.add(tekst(String.format("Periode med %s er fjernet", formaterForHistorikk(fomFjernet)))));
        fjernetOppholdListe.forEach(fjernetOpphold -> endredeLinjer.add(tekst(String.format("Periode med __opphold__ %s er fjernet", formatterOppholdDetaljerForHistorikk(fjernetOpphold)))));

        lagtTilListe.forEach(fomLagtTil -> endredeLinjer.add(tekst(String.format("Lagt til %s ", formaterForHistorikk(fomLagtTil)))));
        lagtTilOppholdListe.forEach(lagtTilOpphold -> endredeLinjer.add(tekst(String.format("Lagt til nytt __opphold__ %s", formatterOppholdDetaljerForHistorikk(lagtTilOpphold)))));
        return endredeLinjer;
    }

    public String lagTekstForArbeidsgiver(SvpTilretteleggingEntitet tilretteleggingEntitet) {
        if (tilretteleggingEntitet.getArbeidsgiver().isPresent()) {
            var opplysninger = arbeidsgiverTjeneste.hent(tilretteleggingEntitet.getArbeidsgiver().get());
            return opplysninger.getNavn() + " (" + opplysninger.getIdentifikator() + ")";
        } else {
            return tilretteleggingEntitet.getArbeidType().getNavn();
        }
    }

    private String formatterOppholdDetaljerForHistorikk(SvpAvklartOpphold opphold) {
        var oppholdÅrsakTekst = switch (opphold.getOppholdÅrsak()) {
            case SYKEPENGER -> "Sykepenger 100%";
            case FERIE -> "Ferie";
        };
        return SvpOppholdKilde.SØKNAD.equals(opphold.getKilde())
            ? String.format("__%s__ med __%s__ og kilde søknad", format(opphold.getTidsperiode()), oppholdÅrsakTekst)
            : String.format("__%s__ med __%s__", format(opphold.getTidsperiode()), oppholdÅrsakTekst);
    }

    private String formaterForHistorikk(TilretteleggingFOM fom) {
        var builder = new StringBuilder(String.format("__%s__ fra og med __%s__", fom.getType().getNavn(), format(fom.getFomDato())));
        if (TilretteleggingType.DELVIS_TILRETTELEGGING.equals(fom.getType())) {
            builder.append(String.format(" med stillingsprosent __%s__", fom.getStillingsprosent()));
        }
        if (fom.getOverstyrtUtbetalingsgrad() != null) {
            builder.append(String.format(" og overstyrt utbetalingsgrad __%s__", fom.getOverstyrtUtbetalingsgrad()));
        }
        return builder.toString();
    }

    private static boolean erTilretteleggingEndret(SvpTilretteleggingEntitet eksisterendeTilrettelegging, SvpTilretteleggingEntitet nyTilrettelegging) {
        var nyFomsSortert = sorterFoms(nyTilrettelegging.getTilretteleggingFOMListe());
        var eksFomsSortert = sorterFoms(eksisterendeTilrettelegging.getTilretteleggingFOMListe());
        var nyOppholdSortert = sorterOpphold(nyTilrettelegging.getAvklarteOpphold());
        var eksOppholdSortert = sorterOpphold(eksisterendeTilrettelegging.getAvklarteOpphold());

        return nyTilrettelegging.getSkalBrukes() != eksisterendeTilrettelegging.getSkalBrukes()
            || !nyFomsSortert.equals(eksFomsSortert)
            || !Objects.equals(eksisterendeTilrettelegging.getBehovForTilretteleggingFom(), nyTilrettelegging.getBehovForTilretteleggingFom())
            || !nyOppholdSortert.equals(eksOppholdSortert);
    }

    private static List<TilretteleggingFOM> sorterFoms(List<TilretteleggingFOM> tilretteleggingFOM) {
        return tilretteleggingFOM.stream()
            .sorted(Comparator.comparing(TilretteleggingFOM::getFomDato))
            .toList();
    }

    private static List<SvpAvklartOpphold> sorterOpphold(List<SvpAvklartOpphold> tilretteleggingFOM) {
        return tilretteleggingFOM.stream()
            .sorted(Comparator.comparing(SvpAvklartOpphold::getFom))
            .toList();
    }

    private static SvpTilretteleggingEntitet hentEksisterendeTilrettelegging(List<SvpTilretteleggingEntitet> eksisterendeTilrettelegingerListe, SvpTilretteleggingEntitet nyTilrettelegging) {
        return eksisterendeTilrettelegingerListe.stream()
            .filter(eksistrende -> erTilrettleggingPåSammeArbeidsforhold(eksistrende, nyTilrettelegging))
            .findFirst()
            .orElseThrow(() -> new TekniskException("FP-572361", "Fant ikke eksiterende tilrettelegging med identifikator" +
                new TilretteleggingAktivitet(
                    nyTilrettelegging.getArbeidsgiver().orElse(null),
                    nyTilrettelegging.getInternArbeidsforholdRef().orElse(null),
                    nyTilrettelegging.getArbeidType())));
    }

    private static boolean erLikeUtenKilde(SvpAvklartOpphold a, SvpAvklartOpphold b) {
        return Objects.equals(a.getOppholdÅrsak(), b.getOppholdÅrsak()) && Objects.equals(a.getFom(), b.getFom()) && Objects.equals(
            a.getTom(), b.getTom());
    }

    private static boolean erTilrettleggingPåSammeArbeidsforhold(SvpTilretteleggingEntitet eksistrende, SvpTilretteleggingEntitet nyTilrettelegging) {
        var eksisterendeAG = new TilretteleggingAktivitet(
            eksistrende.getArbeidsgiver().orElse(null),
            eksistrende.getInternArbeidsforholdRef().orElse(null),
            eksistrende.getArbeidType()
        );
        var nyAG = new TilretteleggingAktivitet(
            nyTilrettelegging.getArbeidsgiver().orElse(null),
            nyTilrettelegging.getInternArbeidsforholdRef().orElse(null),
            nyTilrettelegging.getArbeidType()
        );
        return eksisterendeAG.equals(nyAG);
    }

    private static HistorikkinnslagLinjeBuilder tekst(String tekst) {
        return new HistorikkinnslagLinjeBuilder().tekst(tekst);
    }

    private record TilretteleggingAktivitet(Arbeidsgiver identifikator, InternArbeidsforholdRef internArbeidsforholdRef, ArbeidType arbeidType) {
    }
}
