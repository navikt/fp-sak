package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;

@ApplicationScoped
public class FaktaUttakPeriodeDtoTjeneste {

    private UttakInputTjeneste uttakInputTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public FaktaUttakPeriodeDtoTjeneste(UttakInputTjeneste uttakInputTjeneste,
                                        YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                        BehandlingRepository behandlingRepository) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    FaktaUttakPeriodeDtoTjeneste() {
        //CDI
    }

    public List<FaktaUttakPeriodeDto> lagDtos(UuidDto uuidDto) {
        var behandlingId = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid()).getId();
        var uttakInput = uttakInputTjeneste.lagInput(behandlingId);
        if (!uttakInput.isSkalBrukeNyFaktaOmUttak()) {
            return List.of();
        }
        var ytelseFordelingAggregatOpt = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId);
        if (ytelseFordelingAggregatOpt.isEmpty()) {
            return List.of();
        }
        //TODO TFP-4873 perioder før endringsdato burde vises

        return ytelseFordelingAggregatOpt.get().getGjeldendeFordeling().getPerioder()
            .stream()
            .map(p -> tilDto(p))
            .toList();
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
            periode.getSamtidigUttaksprosent(), periode.isFlerbarnsdager(), morsAktivitet, periode.getPeriodeKilde());
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
