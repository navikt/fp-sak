package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.slåSammenLikePerioder;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperioderHelper;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;

@ApplicationScoped
public class FaktaUttakPeriodeDtoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private BehandlingRepository behandlingRepository;
    private FpUttakRepository uttakRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    public FaktaUttakPeriodeDtoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                        BehandlingRepository behandlingRepository,
                                        FpUttakRepository uttakRepository,
                                        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.uttakRepository = uttakRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    FaktaUttakPeriodeDtoTjeneste() {
        //CDI
    }

    public List<FaktaUttakPeriodeDto> lagDtos(UuidDto uuidDto) {
        var behandlingId = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid()).getId();
        return lagDtos(behandlingId);
    }

    public List<FaktaUttakPeriodeDto> lagDtos(Long behandlingId) {
        return hentRelevanteOppgittPerioder(behandlingId).map(this::tilDto).toList();
    }

    public Stream<OppgittPeriodeEntitet> hentRelevanteOppgittPerioder(Long behandlingId) {
        var ytelseFordelingAggregatOpt = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId);
        if (ytelseFordelingAggregatOpt.isEmpty()) {
            return Stream.of();
        }

        var yfa = ytelseFordelingAggregatOpt.get();
        var perioder = yfa.getGjeldendeFordeling().getPerioder();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erRevurdering()) {
            var uttakOriginalBehandling = uttakRepository.hentUttakResultatHvisEksisterer(behandling.getOriginalBehandlingId().orElseThrow());
            if (uttakOriginalBehandling.isPresent()) {
                var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
                var fraDato = behandlingSomJusteresFarsUttakVedFødsel(behandling, yfa) ?
                    skjæringstidspunkt.getSkjæringstidspunktHvisUtledet().orElse(LocalDate.MIN) : LocalDate.MIN;
                perioder = slåSammenLikePerioder(VedtaksperioderHelper.opprettOppgittePerioder(uttakOriginalBehandling.get(), perioder,
                    fraDato, false));
            }
        }
        return perioder.stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getTom));
    }

    private static boolean behandlingSomJusteresFarsUttakVedFødsel(Behandling behandling, YtelseFordelingAggregat yfa) {
        return RelasjonsRolleType.erFarEllerMedmor(behandling.getRelasjonsRolleType()) && behandling.harBehandlingÅrsak(
            BehandlingÅrsakType.RE_HENDELSE_FØDSEL) && yfa.getGjeldendeFordeling().ønskerJustertVedFødsel();
    }

    private FaktaUttakPeriodeDto tilDto(OppgittPeriodeEntitet periode) {
        var utsettelseÅrsak = periode.isUtsettelse() ? (UtsettelseÅrsak) periode.getÅrsak() : null;
        var oppholdÅrsak = periode.isOpphold() ? (OppholdÅrsak) periode.getÅrsak() : null;
        var overføringÅrsak = periode.isOverføring() ? (OverføringÅrsak) periode.getÅrsak() : null;
        var arbeidsprosent = periode.getArbeidsprosent();
        var arbeidsforhold = arbeidsprosent != null ? mapArbeidsforhold(periode) : null;
        var periodeType = periode.getPeriodeType() == null || UttakPeriodeType.UDEFINERT.equals(periode.getPeriodeType()) ? null : periode.getPeriodeType();
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
