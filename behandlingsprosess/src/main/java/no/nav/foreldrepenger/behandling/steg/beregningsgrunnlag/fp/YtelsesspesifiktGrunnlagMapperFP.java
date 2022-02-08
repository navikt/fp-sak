package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.modell.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.modell.gradering.AndelGradering;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelsegrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetGraderingDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AndelGraderingDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.GraderingDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelseandel;
import no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelseperiode;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.RelatertYtelseType;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.mappers.kalkulatorinput.YtelsesspesifiktGrunnlagMapper;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class YtelsesspesifiktGrunnlagMapperFP implements YtelsesspesifiktGrunnlagMapper {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private BeregningUttakTjeneste beregningUttakTjeneste;
    protected BehandlingRepository behandlingRepository;

    protected YtelsesspesifiktGrunnlagMapperFP() {
        // CDI proxy
    }

    @Inject
    public YtelsesspesifiktGrunnlagMapperFP(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                            BeregningUttakTjeneste beregningUttakTjeneste,
                                            BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste) {

        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.fagsakRelasjonRepository = Objects.requireNonNull(behandlingRepositoryProvider.getFagsakRelasjonRepository(),
            "fagsakRelasjonRepository");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.beregningUttakTjeneste = Objects.requireNonNull(beregningUttakTjeneste, "andelGrderingTjeneste");
    }

    @Override
    public YtelsespesifiktGrunnlagDto mapYtelsesspesifiktGrunnlag(BehandlingReferanse behandlingReferanse) {
        var aktivitetGradering = beregningUttakTjeneste.finnAktivitetGraderinger(behandlingReferanse);
        var saksnummer = behandlingReferanse.getSaksnummer();
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(saksnummer);
        var dekningsgrad = fagsakRelasjon.map(FagsakRelasjon::getGjeldendeDekningsgrad)
            .orElseThrow(() -> new IllegalStateException("Mangler FagsakRelasjon#dekningsgrad for behandling: " + behandlingReferanse));
        var kvalifisererTilBesteberegning = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse);
        var sisteSøkteUttaksdag = beregningUttakTjeneste.finnSisteTilnærmedeUttaksdato(behandlingReferanse);
        var fpGrunnlag = new ForeldrepengerGrunnlag(BigDecimal.valueOf(dekningsgrad.getVerdi()), kvalifisererTilBesteberegning,
            mapAktivitetGradering(aktivitetGradering), sisteSøkteUttaksdag.orElse(null),
            kvalifisererTilBesteberegning ? getBesteberegningYtelsegrunnlag(behandlingReferanse) : null);
        // TODO Legg til behandlingstidspunkt i kontrakt for Foreldrepenger

        return fpGrunnlag;
    }

    private List<no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelsegrunnlag> getBesteberegningYtelsegrunnlag(BehandlingReferanse behandlingReferanse) {
        var ytelsegrunnlag = besteberegningFødendeKvinneTjeneste.lagBesteberegningYtelseinput(behandlingReferanse);
        return ytelsegrunnlag.stream().map(this::mapYtelseGrunnlag).toList();
    }

    private no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelsegrunnlag mapYtelseGrunnlag(Ytelsegrunnlag y) {
        return new no.nav.folketrygdloven.kalkulus.beregning.v1.besteberegning.Ytelsegrunnlag(new RelatertYtelseType(y.getYtelse().getKode()),
            y.getPerioder().stream().map(this::mapYtelsePeriode).toList());
    }

    private Ytelseperiode mapYtelsePeriode(no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelseperiode p) {
        return new Ytelseperiode(new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()),
            p.getAndeler().stream().map(this::mapAndel).toList());
    }

    private Ytelseandel mapAndel(no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelseandel a) {
        return new Ytelseandel(a.getAktivitetStatus(), a.getArbeidskategori(), a.getDagsats());
    }

    private AktivitetGraderingDto mapAktivitetGradering(AktivitetGradering aktivitetGradering) {
        return new AktivitetGraderingDto(mapAndelGradering(aktivitetGradering));
    }

    private List<AndelGraderingDto> mapAndelGradering(AktivitetGradering aktivitetGradering) {
        return aktivitetGradering.getAndelGradering()
            .stream()
            .map(a -> new AndelGraderingDto(a.getAktivitetStatus(),
                a.getArbeidsgiver() != null ? new Organisasjon(a.getArbeidsgiver().getIdentifikator()) : null,
                a.getArbeidsforholdRef() != null ? new InternArbeidsforholdRefDto(a.getArbeidsforholdRef().getReferanse()) : null,
                a.getGraderinger().stream().map(this::mapGradering).toList()))
            .toList();
    }

    private GraderingDto mapGradering(AndelGradering.Gradering g) {
        return new GraderingDto(new Periode(g.getPeriode().getFomDato(), g.getPeriode().getTomDato()), g.getArbeidstidProsent());
    }
}
