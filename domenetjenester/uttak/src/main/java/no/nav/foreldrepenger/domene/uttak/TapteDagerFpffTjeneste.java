package no.nav.foreldrepenger.domene.uttak;


import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

@ApplicationScoped
public class TapteDagerFpffTjeneste {

    private FpUttakRepository fpUttakRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public TapteDagerFpffTjeneste(UttakRepositoryProvider repositoryProvider, YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    TapteDagerFpffTjeneste() {
        //CDI
    }


    public int antallTapteDagerFpff(UttakInput input, int gjenværendeFpff) {
        if (!harSøktFpff(input)) {
            return 0;
        }

        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var familieHendelser = fpGrunnlag.getFamilieHendelser();
        if (harSøktPåTermin(familieHendelser) && skjeddFødselFørTermin(fpGrunnlag)) {
            var virkedager = Virkedager.beregnAntallVirkedager(familieHendelser.getGjeldendeFamilieHendelse().getFødselsdato().orElseThrow(),
                familieHendelser.getGjeldendeFamilieHendelse().getTermindato().orElseThrow().minusDays(1));
            var antallSøkteDagerFpff = antallSøkteDagerFpff(input);
            return Math.min(antallSøkteDagerFpff, Math.min(virkedager, gjenværendeFpff));
        }
        return 0;
    }

    private boolean harSøktFpff(UttakInput input) {
        return antallSøkteDagerFpff(input) > 0;
    }

    private int antallSøkteDagerFpff(UttakInput input) {
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var originalBehandling = fpGrunnlag.getOriginalBehandling();
        if (originalBehandling.isEmpty()) {
            var yf = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(input.getBehandlingReferanse().behandlingId());
            if (yf.isEmpty() || yf.get().getOppgittFordeling() == null) {
                return 0;
            }
            return antallSøkteDagerFpff(yf.get());
        }
        var uttakOriginalBehandling = fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling.get().getId());
        return uttakOriginalBehandling.map(TapteDagerFpffTjeneste::antallSøkteDagerFpff).orElse(0);
    }

    private static Integer antallSøkteDagerFpff(UttakResultatEntitet uttak) {
        return uttak
            .getGjeldendePerioder()
            .getPerioder()
            .stream()
            .filter(p -> p.getAktiviteter().stream().anyMatch(a -> Objects.equals(a.getTrekkonto(), StønadskontoType.FORELDREPENGER_FØR_FØDSEL)))
            .map(p -> Virkedager.beregnAntallVirkedager(p.getFom(), p.getTom()))
            .reduce(Integer::sum)
            .orElse(0);
    }

    private static Integer antallSøkteDagerFpff(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat
            .getOppgittFordeling()
            .getPerioder()
            .stream()
            .filter(p -> Objects.equals(p.getPeriodeType(), UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL))
            .map(p -> Virkedager.beregnAntallVirkedager(p.getFom(), p.getTom()))
            .reduce(Integer::sum)
            .orElse(0);
    }

    private boolean harSøktPåTermin(FamilieHendelser familieHendelser) {
        return familieHendelser.getSøknadFamilieHendelse().getFødselsdato().isEmpty();
    }

    private boolean skjeddFødselFørTermin(ForeldrepengerGrunnlag fpGrunnlag) {
        var familiehendelser = fpGrunnlag.getFamilieHendelser();
        var termindato = familiehendelser.getGjeldendeFamilieHendelse().getTermindato().orElseThrow();
        var gjeldendeFødselsdato = familiehendelser.getGjeldendeFamilieHendelse().getFødselsdato();
        return gjeldendeFødselsdato.isPresent() && gjeldendeFødselsdato.get().isBefore(termindato);
    }
}
