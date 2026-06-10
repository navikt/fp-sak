package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.svp.BekreftSvangerskapspengerOppdaterer.getFødselsdato;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.svp.BekreftSvangerskapspengerOppdaterer.getTermindato;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpAvklartOpphold;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class BekreftSvangerskapspengerHistorikkinnslagTjeneste {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BekreftSvangerskapspengerHistorikkinnslagTjeneste() {
        //CDI
    }

    @Inject
    public BekreftSvangerskapspengerHistorikkinnslagTjeneste(ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                                             HistorikkinnslagRepository historikkinnslagRepository,
                                                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikkinnslagVedEndring(BehandlingReferanse ref,
                                              BekreftSvangerskapspengerDto dto,
                                              FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag,
                                              List<TilretteleggingEndring> endredeTilrettelegginger) {

        var arbeidsforholdInformasjon = inntektArbeidYtelseTjeneste.finnGrunnlag(ref.behandlingId())
            .flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon);

        var builder = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.PUNKT_FOR_SVP_INNGANG)
            .addLinje(fraTilEquals("Termindato", getTermindato(familieHendelseGrunnlag, ref), dto.getTermindato()))
            .addLinje(fraTilEquals("Fødselsdato", getFødselsdato(familieHendelseGrunnlag).orElse(null), dto.getFødselsdato()));

        var antallSplittedeTilrettelegginger = endredeTilrettelegginger.stream()
            .filter(et -> et.endringType() == TilretteleggingEndring.EndringType.SPLITT)
            .toList();

        if (!antallSplittedeTilrettelegginger.isEmpty()) {
            builder.addLinje(String.format("Tilrettelegging er blitt splittet fra 1 til %s tilrettelegginger", antallSplittedeTilrettelegginger));
        }
        endredeTilrettelegginger.stream()
                .filter(et -> et.endringType() == TilretteleggingEndring.EndringType.REVERSER_SPLITT)
                .forEach(et -> builder.addLinje(
                        String.format("Splittet tilrettelegging er reversert fra %s tilrettelegginger til 1", et.gammelTilrettelegging().size())));

        endredeTilrettelegginger.stream()
            .filter(TilretteleggingEndring::skalOppdateres)
            .map(endring -> opprettHistorikkinnslagForTilretteleggingsperiode(endring, arbeidsforholdInformasjon))
            .forEach(t -> {
            builder.addLinje(HistorikkinnslagLinjeBuilder.LINJESKIFT);
            t.forEach(builder::addLinje);
            builder.addLinje(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        });

        builder.addLinje(dto.getBegrunnelse());
        historikkinnslagRepository.lagre(builder.build());
    }

    private List<HistorikkinnslagLinjeBuilder> opprettHistorikkinnslagForTilretteleggingsperiode(TilretteleggingEndring endringStatus,
                                                                                                 Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        var eksisterendeTilrettelegging = endringStatus.getGammelTilrettelegging();
        var nyTilrettelegging = endringStatus.nyTilrettelegging();

        var endredeLinjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        endredeLinjer.add(new HistorikkinnslagLinjeBuilder().tekst(lagTekstForArbeidsgiver(nyTilrettelegging, arbeidsforholdInformasjon)));

        endredeLinjer.add(
            fraTilEquals("Tilrettelegging skal brukes", eksisterendeTilrettelegging.map(SvpTilretteleggingEntitet::getSkalBrukes).orElse(null),
                nyTilrettelegging.getSkalBrukes()));
        endredeLinjer.add(fraTilEquals("Tilrettelegging er nødvendig fra og med",
            eksisterendeTilrettelegging.map(SvpTilretteleggingEntitet::getBehovForTilretteleggingFom).orElse(null),
            nyTilrettelegging.getBehovForTilretteleggingFom()));

        // Tilrettelegging fra datoer
        var eksisterendeFoms = eksisterendeTilrettelegging.map(SvpTilretteleggingEntitet::getTilretteleggingFOMListe).orElse(List.of());
        var nyeFoms = nyTilrettelegging.getTilretteleggingFOMListe();
        var fjernetListe = eksisterendeFoms.stream().filter(fom -> !nyeFoms.contains(fom)).toList();
        var lagtTilListe = nyeFoms.stream().filter(fom -> !eksisterendeFoms.contains(fom)).toList();

        // Oppholdsperioder
        var eksisterendeOpphold = eksisterendeTilrettelegging.map(SvpTilretteleggingEntitet::getAvklarteOpphold).orElse(List.of());
        var nyeOpphold = nyTilrettelegging.getAvklarteOpphold();
        var fjernetOppholdListe = eksisterendeOpphold.stream().filter(eksisterende -> nyeOpphold.stream().noneMatch(nyttOpphold -> erLikeUtenKilde(nyttOpphold, eksisterende)))
            .toList();
        var lagtTilOppholdListe = nyeOpphold.stream()
            .filter(nyttOpphold -> eksisterendeOpphold.stream().noneMatch(eksisterende -> erLikeUtenKilde(eksisterende, nyttOpphold)))
            .toList();

        fjernetListe.forEach(fomFjernet -> endredeLinjer.add(
            tekst(String.format("Periode med %s er fjernet", formaterForHistorikk(fomFjernet)))));
        fjernetOppholdListe.forEach(fjernetOpphold -> endredeLinjer.add(
            tekst(String.format("Periode med __opphold__ %s er fjernet", formatterOppholdDetaljerForHistorikk(fjernetOpphold)))));

        lagtTilListe.forEach(fomLagtTil -> endredeLinjer.add(tekst(String.format("Lagt til %s ", formaterForHistorikk(fomLagtTil)))));
        lagtTilOppholdListe.forEach(lagtTilOpphold -> endredeLinjer.add(
            tekst(String.format("Lagt til nytt __opphold__ %s", formatterOppholdDetaljerForHistorikk(lagtTilOpphold)))));
        return endredeLinjer;
    }

    private String lagTekstForArbeidsgiver(SvpTilretteleggingEntitet tilretteleggingEntitet,
                                           Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        var arbeidsgiver = tilretteleggingEntitet.getArbeidsgiver();
        if (arbeidsgiver.isPresent()) {
            var opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver.get());
            var eksternRef = finnEksternArbeidsforholdRef(tilretteleggingEntitet, arbeidsgiver.get(), arbeidsforholdInformasjon);
            return ArbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(opplysninger, eksternRef);
        } else {
            return tilretteleggingEntitet.getArbeidType().getNavn();
        }
    }

    private Optional<EksternArbeidsforholdRef> finnEksternArbeidsforholdRef(SvpTilretteleggingEntitet tilrettelegging,
                                                                            Arbeidsgiver arbeidsgiver,
                                                                            Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        return tilrettelegging.getInternArbeidsforholdRef()
            .filter(InternArbeidsforholdRef::gjelderForSpesifiktArbeidsforhold)
            .flatMap(internRef -> arbeidsforholdInformasjon.map(ai -> ai.finnEkstern(arbeidsgiver, internRef)));
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


    private static boolean erLikeUtenKilde(SvpAvklartOpphold a, SvpAvklartOpphold b) {
        return Objects.equals(a.getOppholdÅrsak(), b.getOppholdÅrsak()) && Objects.equals(a.getFom(), b.getFom()) && Objects.equals(
            a.getTom(), b.getTom());
    }

    private static HistorikkinnslagLinjeBuilder tekst(String tekst) {
        return new HistorikkinnslagLinjeBuilder().tekst(tekst);
    }

}
