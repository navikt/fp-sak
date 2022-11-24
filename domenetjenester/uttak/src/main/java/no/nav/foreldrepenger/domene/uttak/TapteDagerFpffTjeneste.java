package no.nav.foreldrepenger.domene.uttak;


import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;

@ApplicationScoped
public class TapteDagerFpffTjeneste {

    private FpUttakRepository fpUttakRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;

    @Inject
    public TapteDagerFpffTjeneste(UttakRepositoryProvider repositoryProvider, YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
    }

    TapteDagerFpffTjeneste() {
        //CDI
    }


    public int antallTapteDagerFpff(UttakInput input) {
        if (!harSøktFpff(input)) {
            return 0;
        }

        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var familieHendelser = fpGrunnlag.getFamilieHendelser();
        if (harSøktPåTermin(familieHendelser) && skjeddFødselFørTermin(fpGrunnlag)) {
            var virkedager = Virkedager.beregnAntallVirkedager(familieHendelser.getGjeldendeFamilieHendelse().getFødselsdato().orElseThrow(),
                familieHendelser.getGjeldendeFamilieHendelse().getTermindato().orElseThrow().minusDays(1));
            var maxdagerFpff = finnMaksdagerFpff(input.getBehandlingReferanse().saksnummer());
            return Math.min(virkedager, maxdagerFpff);
        }
        return 0;
    }

    private int finnMaksdagerFpff(Saksnummer saksnummer) {
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(saksnummer);
        if (fagsakRelasjon.isPresent()) {
            var stønadskontoberegning = fagsakRelasjon.get().getGjeldendeStønadskontoberegning();
            if (stønadskontoberegning.isPresent()) {
                var fpffKonto = stønadskontoberegning.get().getStønadskontoer()
                    .stream()
                    .filter(stønadskonto -> stønadskonto.getStønadskontoType().equals(StønadskontoType.FORELDREPENGER_FØR_FØDSEL))
                    .findFirst();
                if (fpffKonto.isPresent()) {
                    return fpffKonto.get().getMaxDager();
                }
            }
        }
        return 0;
    }

    private boolean harSøktFpff(UttakInput input) {
        return originalBehandlingHarFpff(input) || søktFpffIAktivBehandling(input);
    }

    private boolean søktFpffIAktivBehandling(UttakInput input) {
        var yf = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(input.getBehandlingReferanse().behandlingId());
        if (yf.isEmpty() || yf.get().getOppgittFordeling() == null) {
            return false;
        }
        return yf.get().getOppgittFordeling().getPerioder().stream()
            .anyMatch(p -> Objects.equals(p.getPeriodeType(), UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL));
    }

    private boolean originalBehandlingHarFpff(UttakInput input) {
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var originalBehandling = fpGrunnlag.getOriginalBehandling();
        if (originalBehandling.isEmpty()) {
            return false;
        }
        var uttakOriginalBehandling = fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling.get().getId());
        if (uttakOriginalBehandling.isEmpty()) {
            return false;
        }
        return uttakOriginalBehandling.get().getGjeldendePerioder().getPerioder().stream()
            .anyMatch(p -> p.getAktiviteter().stream()
                .anyMatch(a -> Objects.equals(a.getTrekkonto(), StønadskontoType.FORELDREPENGER_FØR_FØDSEL)));
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
