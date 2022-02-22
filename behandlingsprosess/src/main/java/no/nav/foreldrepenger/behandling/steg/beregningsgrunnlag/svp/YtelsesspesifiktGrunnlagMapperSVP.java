package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.svp;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.SvangerskapspengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.BeregnTilrettleggingsperioderTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.TilretteleggingMedUtbelingsgrad;
import no.nav.foreldrepenger.domene.mappers.kalkulatorinput.YtelsesspesifiktGrunnlagMapper;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.TilretteleggingMapperTilKalkulus;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
class YtelsesspesifiktGrunnlagMapperSVP implements YtelsesspesifiktGrunnlagMapper {

    private BeregnTilrettleggingsperioderTjeneste tilrettleggingsperioderTjeneste;

    public YtelsesspesifiktGrunnlagMapperSVP() {
    }

    @Inject
    public YtelsesspesifiktGrunnlagMapperSVP(BeregnTilrettleggingsperioderTjeneste tilrettleggingsperioderTjeneste) {
        this.tilrettleggingsperioderTjeneste = tilrettleggingsperioderTjeneste;
    }

    @Override
    public YtelsespesifiktGrunnlagDto mapYtelsesspesifiktGrunnlag(BehandlingReferanse behandlingReferanse) {
        var tilretteleggingMedUtbelingsgrad = tilrettleggingsperioderTjeneste.beregnPerioder(behandlingReferanse);
        return new SvangerskapspengerGrunnlag(mapTilrettelegginger(tilretteleggingMedUtbelingsgrad));
    }

    private List<UtbetalingsgradPrAktivitetDto> mapTilrettelegginger(List<TilretteleggingMedUtbelingsgrad> tilretteleggingMedUtbelingsgrad) {
        return TilretteleggingMapperTilKalkulus.mapTilretteleggingerMedUtbetalingsgrad(tilretteleggingMedUtbelingsgrad)
            .stream()
            .map(this::mapUtbetalingsgradPrAktivitet)
            .toList();
    }

    private UtbetalingsgradPrAktivitetDto mapUtbetalingsgradPrAktivitet(no.nav.folketrygdloven.kalkulator.modell.svp.UtbetalingsgradPrAktivitetDto t) {
        return new UtbetalingsgradPrAktivitetDto(mapAktivitet(t.getUtbetalingsgradArbeidsforhold()), mapPerioder(t.getPeriodeMedUtbetalingsgrad()));
    }

    private List<PeriodeMedUtbetalingsgradDto> mapPerioder(List<no.nav.folketrygdloven.kalkulator.modell.svp.PeriodeMedUtbetalingsgradDto> periodeMedUtbetalingsgrad) {
        return periodeMedUtbetalingsgrad.stream().map(this::mapPeriode).toList();
    }

    private PeriodeMedUtbetalingsgradDto mapPeriode(no.nav.folketrygdloven.kalkulator.modell.svp.PeriodeMedUtbetalingsgradDto p) {
        return new PeriodeMedUtbetalingsgradDto(new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()), p.getUtbetalingsgrad());
    }

    private AktivitetDto mapAktivitet(no.nav.folketrygdloven.kalkulator.modell.svp.AktivitetDto aktivitet) {
        return new AktivitetDto(mapArbeidsgiver(aktivitet.getArbeidsgiver()),
            aktivitet.getInternArbeidsforholdRef() != null ? new InternArbeidsforholdRefDto(
                aktivitet.getInternArbeidsforholdRef().getReferanse()) : null, new UttakArbeidType(aktivitet.getUttakArbeidType().getKode()));
    }

    private Aktør mapArbeidsgiver(Optional<Arbeidsgiver> arbeidsgiver) {
        return arbeidsgiver.map(a -> a.getErVirksomhet() ? new Organisasjon(a.getIdentifikator()) : new AktørIdPersonident(a.getIdentifikator()))
            .orElse(null);
    }
}
