package no.nav.foreldrepenger.ytelse.beregning.adapter;

import static no.nav.foreldrepenger.ytelse.beregning.MapAkivitetStatusFraBehandlingslagerTilBeregningsgrunnlag.mapAktivitetStatus;
import static no.nav.foreldrepenger.ytelse.beregning.MapInntektskategoriFraBehandlingslagerTilBeregningsgrunnlag.mapInntektskategori;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.MapInntektskategoriFraBehandlingslagerTilBeregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class MapBeregningsresultatFeriepengerFraVLTilRegel {


    MapBeregningsresultatFeriepengerFraVLTilRegel() {
        //Skal ikke instansieres
    }

    public static BeregningsresultatFeriepengerRegelModell mapFra(Behandling behandling, BeregningsresultatEntitet beregningsresultat,
                                                                  Optional<BeregningsresultatEntitet> annenPartsBeregningsresultatFP,
                                                                  Dekningsgrad dekningsgrad,
                                                                  int antallDagerFeriepenger) {

        var annenPartsBeregningsresultatPerioder = annenPartsBeregningsresultatFP.map(a -> a.getBeregningsresultatPerioder().stream()
            .map(MapBeregningsresultatFeriepengerFraVLTilRegel::mapBeregningsresultatPerioder)
            .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
        var beregningsresultatPerioder = beregningsresultat.getBeregningsresultatPerioder().stream()
            .map(MapBeregningsresultatFeriepengerFraVLTilRegel::mapBeregningsresultatPerioder).collect(Collectors.toList());
        var inntektskategorier = mapInntektskategorier(beregningsresultat);
        var annenPartsInntektskategorier = annenPartsBeregningsresultatFP.map(MapBeregningsresultatFeriepengerFraVLTilRegel::mapInntektskategorier).orElse(Collections.emptySet());
        var erForelder1 = RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType());

        return BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(beregningsresultatPerioder)
            .medInntektskategorier(inntektskategorier)
            .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
            .medAnnenPartsInntektskategorier(annenPartsInntektskategorier)
            .medDekningsgrad(map(dekningsgrad))
            .medErForelder1(erForelder1)
            .medAntallDagerFeriepenger(antallDagerFeriepenger)
            .build();
    }

    private static no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Dekningsgrad map(Dekningsgrad dekningsgrad) {
        if (Objects.equals(dekningsgrad.getVerdi(), Dekningsgrad._80.getVerdi())) {
            return no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Dekningsgrad.DEKNINGSGRAD_80;
        }

        if (Objects.equals(dekningsgrad.getVerdi(), Dekningsgrad._100.getVerdi())) {
            return no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Dekningsgrad.DEKNINGSGRAD_100;
        }
        throw new IllegalStateException("Ukjent dekningsgrad " + dekningsgrad.getVerdi());
    }

    private static Set<Inntektskategori> mapInntektskategorier(BeregningsresultatEntitet beregningsresultat) {
        return beregningsresultat.getBeregningsresultatPerioder().stream()
            .flatMap(periode -> periode.getBeregningsresultatAndelList().stream())
            .filter(andel -> andel.getDagsats() > 0)
            .map(no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel::getInntektskategori)
            .map(MapInntektskategoriFraBehandlingslagerTilBeregningsgrunnlag::mapInntektskategori)
            .map(InntektskategoriMapper::fraVLTilRegel)
            .collect(Collectors.toSet());
    }

    private static BeregningsresultatPeriode mapBeregningsresultatPerioder(no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode beregningsresultatPerioder) {
        var periode = BeregningsresultatPeriode.builder()
            .medPeriode(new LocalDateInterval(beregningsresultatPerioder.getBeregningsresultatPeriodeFom(), beregningsresultatPerioder.getBeregningsresultatPeriodeTom()))
            .build();
        beregningsresultatPerioder.getBeregningsresultatAndelList().forEach(andel -> mapBeregningsresultatAndel(andel, periode));
        return periode;
    }

    private static void mapBeregningsresultatAndel(no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel andel, BeregningsresultatPeriode periode) {
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(andel.erBrukerMottaker())
            .medDagsats((long) andel.getDagsats())
            .medDagsatsFraBg((long) andel.getDagsatsFraBg())
            .medAktivitetStatus(AktivitetStatusMapper.fraVLTilRegel(mapAktivitetStatus(andel.getAktivitetStatus())))
            .medInntektskategori(InntektskategoriMapper.fraVLTilRegel(mapInntektskategori(andel.getInntektskategori())))
            .medArbeidsforhold(mapArbeidsforhold(andel))
            .build(periode);
    }

    private static Arbeidsforhold mapArbeidsforhold(no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel andel) {
        if (andel.getAktivitetStatus().erFrilanser()) {
            return Arbeidsforhold.frilansArbeidsforhold();
        }
        if (!andel.getArbeidsgiver().isPresent()) {
            return null;
        }
        return lagArbeidsforholdHosArbeidsgiver(andel.getArbeidsgiver().get(), andel.getArbeidsforholdRef());
    }

    private static Arbeidsforhold lagArbeidsforholdHosArbeidsgiver(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        if (arbeidsgiver.erAktørId()) {
            return arbeidsforholdRef == null
                ? Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbeidsgiver.getIdentifikator())
                : Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbeidsgiver.getIdentifikator(), arbeidsforholdRef.getReferanse());
        }
        if (arbeidsgiver.getErVirksomhet()) {
            return arbeidsforholdRef == null
                ? Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbeidsgiver.getIdentifikator())
                : Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbeidsgiver.getIdentifikator(), arbeidsforholdRef.getReferanse());
        }
        throw new IllegalStateException("Arbeidsgiver har ingen av de forventede identifikatorene");
    }
}
