package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.slåSammenLikePerioder;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperioderHelper;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;

@ApplicationScoped
public class FaktaUttakPeriodeDtoTjeneste {

    private UttakInputTjeneste uttakInputTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private BehandlingRepository behandlingRepository;
    private FpUttakRepository uttakRepository;

    @Inject
    public FaktaUttakPeriodeDtoTjeneste(UttakInputTjeneste uttakInputTjeneste,
                                        YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                        BehandlingRepository behandlingRepository,
                                        FpUttakRepository uttakRepository) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.uttakRepository = uttakRepository;
    }

    FaktaUttakPeriodeDtoTjeneste() {
        //CDI
    }

    public List<FaktaUttakPeriodeDto> lagDtos(UuidDto uuidDto) {
        var behandlingId = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid()).getId();
        return lagDtos(behandlingId);
    }

    public List<FaktaUttakPeriodeDto> lagDtos(Long behandlingId) {
        return hentRelevanteOppgittPerioder(behandlingId).map(p -> tilDto(p)).toList();
    }

    public Stream<OppgittPeriodeEntitet> hentRelevanteOppgittPerioder(Long behandlingId) {
        var uttakInput = uttakInputTjeneste.lagInput(behandlingId);
        if (!uttakInput.isSkalBrukeNyFaktaOmUttak()) {
            return Stream.of();
        }
        var ytelseFordelingAggregatOpt = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId);
        if (ytelseFordelingAggregatOpt.isEmpty()) {
            return Stream.of();
        }

        var yfa = ytelseFordelingAggregatOpt.get();
        var perioder = yfa.getGjeldendeFordeling().getPerioder();
        var behandlingReferanse = uttakInput.getBehandlingReferanse();
        if (behandlingReferanse.erRevurdering()) {
            var uttakOriginalBehandling = uttakRepository.hentUttakResultatHvisEksisterer(behandlingReferanse
                .originalBehandlingId());
            if (uttakOriginalBehandling.isPresent()) {
                var fraDato = behandlingSomJusteresFarsUttakVedFødsel(uttakInput, yfa, behandlingReferanse) ?
                    behandlingReferanse.getUtledetSkjæringstidspunktHvisUtledet().orElse(LocalDate.MIN) : LocalDate.MIN;
                perioder = slåSammenLikePerioder(VedtaksperioderHelper.opprettOppgittePerioder(uttakOriginalBehandling.get(), perioder,
                    fraDato, behandlingReferanse.getSkjæringstidspunkt().kreverSammenhengendeUttak()));
            }
        }
        return perioder.stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getTom));
    }

    private static boolean behandlingSomJusteresFarsUttakVedFødsel(UttakInput uttakInput, YtelseFordelingAggregat yfa, BehandlingReferanse behandlingReferanse) {
        return RelasjonsRolleType.erFarEllerMedmor(behandlingReferanse.relasjonRolle()) && uttakInput.harBehandlingÅrsak(
            BehandlingÅrsakType.RE_HENDELSE_FØDSEL) && yfa.getGjeldendeFordeling().ønskerJustertVedFødsel();
    }

    private FaktaUttakPeriodeDto tilDto(OppgittPeriodeEntitet periode) {
        var utsettelseÅrsak = periode.isUtsettelse() ? (UtsettelseÅrsak) periode.getÅrsak() : null;
        var oppholdÅrsak = periode.isOpphold() ? (OppholdÅrsak) periode.getÅrsak() : null;
        var overføringÅrsak = periode.isOverføring() ? (OverføringÅrsak) periode.getÅrsak() : null;
        var arbeidsprosent = periode.getArbeidsprosent();
        var arbeidsforhold = arbeidsprosent != null ? mapArbeidsforhold(periode) : null;
        var periodeType = periode.getPeriodeType() == null || Set.of(UttakPeriodeType.ANNET, UttakPeriodeType.UDEFINERT).contains(
            periode.getPeriodeType()) ? null : periode.getPeriodeType();
        var morsAktivitet = periode.getMorsAktivitet() == null || MorsAktivitet.UDEFINERT.equals(periode.getMorsAktivitet()) ? null : periode.getMorsAktivitet();
        return new FaktaUttakPeriodeDto(periode.getFom(), periode.getTom(),
            periodeType, utsettelseÅrsak, overføringÅrsak, oppholdÅrsak, arbeidsprosent, arbeidsforhold,
            periode.getSamtidigUttaksprosent(), periode.isFlerbarnsdager(), morsAktivitet, periode.getPeriodeKilde(),
            periode.getBegrunnelse().orElse(null));
    }

    private ArbeidsforholdDto mapArbeidsforhold(OppgittPeriodeEntitet periode) {
        var arbeidsgiverReferanse = arbeidsgiverReferanse(periode);
        return new ArbeidsforholdDto(arbeidsgiverReferanse, mapUttakArbeidType(periode));
    }

    private UttakArbeidType mapUttakArbeidType(OppgittPeriodeEntitet periode) {
        if (periode.getGraderingAktivitetType() == null) {
            return null;
        }
        return switch (periode.getGraderingAktivitetType()) {
            case ARBEID -> UttakArbeidType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> UttakArbeidType.FRILANS;
        };
    }

    private String arbeidsgiverReferanse(OppgittPeriodeEntitet oppgittPeriode) {
        var arbeidsgiver = oppgittPeriode.getArbeidsgiver();
        return arbeidsgiver == null ?  null : arbeidsgiver.getIdentifikator();
    }

}
