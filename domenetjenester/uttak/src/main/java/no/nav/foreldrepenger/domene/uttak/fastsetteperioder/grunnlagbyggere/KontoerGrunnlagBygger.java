package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Konto;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Kontoer;

@ApplicationScoped
public class KontoerGrunnlagBygger {

    // TODO (TFP-4846) flytt til Stønadskontoberegning og lagre på FR el Fagsak
    private static final int MINSTEDAGER_UFØRE_100_PROSENT = 75;
    private static final int MINSTEDAGER_UFØRE_80_PROSENT = 95;

    private static final int BFHR_MINSTERETT_DAGER = 40;
    private static final int UTTAK_RUNDT_FØDSEL_DAGER = 10;


    private FagsakRelasjonRepository fagsakRelasjonRepository;

    @Inject
    public KontoerGrunnlagBygger(UttakRepositoryProvider repositoryProvider) {
        fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
    }

    KontoerGrunnlagBygger() {
        //CDI
    }

    /*
     * Ved siden av kontoer kan grunnlaget inneholde enten utenAktivitetskravDager eller minsterettDager, men ikke begge
     *
     * utenAktivitetskravDager
     * - gir mulighet til å innvilge perioder selv om aktivitetskravet ikke er oppfylt
     * - vil ikke påvirke stønadsperioden dvs må tas ut fortløpende. Ingen utsettelse uten at aktivitetskrav oppfylt
     * - Skal alltid brukes på tilfelle som krever sammenhengende uttak
     *
     * minsterettDager
     * - gir mulighet til å innvilge perioder selv om aktivitetskravet ikke er oppfylt
     * - automatiske trekk pga manglende søkt, avslag mv vil ikke påvirke minsterett
     * - kan utsettes og  utvide stønadsperioden
     * - Brukes framfor utenAktivitetskravDager fom FAB
     */
    public Kontoer.Builder byggGrunnlag(BehandlingReferanse ref, ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var stønadskontoer = hentStønadskontoer(ref);
        return getBuilder(ref, foreldrepengerGrunnlag, stønadskontoer).kontoList(stønadskontoer.stream()
            //Flerbarnsdager er stønadskontotype i stønadskontoberegningen, men ikke i fastsette perioder
            .filter(sk -> !sk.getStønadskontoType().equals(StønadskontoType.FLERBARNSDAGER))
            .map(this::map)
            .collect(Collectors.toList()));
    }

    private Konto.Builder map(Stønadskonto stønadskonto) {
        return new Konto.Builder()
            .trekkdager(stønadskonto.getMaxDager())
            .type(UttakEnumMapper.map(stønadskonto.getStønadskontoType()));
    }

    private Set<Stønadskonto> hentStønadskontoer(BehandlingReferanse ref) {
        return fagsakRelasjonRepository.finnRelasjonFor(ref.getSaksnummer()).getGjeldendeStønadskontoberegning()
            .orElseThrow(() -> new IllegalArgumentException("Behandling mangler stønadskontoer"))
            .getStønadskontoer();
    }

    /*
     * TFP-4846 legge inn regler for minsterett i stønadskontoutregningen
     */
    private Kontoer.Builder getBuilder(BehandlingReferanse ref, ForeldrepengerGrunnlag foreldrepengerGrunnlag, Set<Stønadskonto> stønadskontoer) {
        var builder = new Kontoer.Builder();
        var erMor = RelasjonsRolleType.MORA.equals(ref.getRelasjonsRolleType());
        var erForeldrepenger = stønadskontoer.stream().map(Stønadskonto::getStønadskontoType).anyMatch(StønadskontoType.FORELDREPENGER::equals);
        var minsterettFarMedmor = !ref.getSkjæringstidspunkt().utenMinsterett();
        var morHarUføretrygd = foreldrepengerGrunnlag.getUføretrygdGrunnlag()
            .filter(UføretrygdGrunnlagEntitet::annenForelderMottarUføretrygd)
            .isPresent();
        var flerbarnsdager = stønadskontoer.stream()
            .filter(stønadskonto -> stønadskonto.getStønadskontoType().equals(StønadskontoType.FLERBARNSDAGER))
            .findFirst();
        flerbarnsdager.map(stønadskonto -> builder.flerbarnsdager(stønadskonto.getMaxDager()));

        if (!erMor && erForeldrepenger && (minsterettFarMedmor || morHarUføretrygd)) {
            var dekningsgrad = fagsakRelasjonRepository.finnRelasjonFor(ref.getSaksnummer()).getGjeldendeDekningsgrad();
            var antallDager = 0;
            if (minsterettFarMedmor) {
                antallDager = BFHR_MINSTERETT_DAGER;
            }
            if (morHarUføretrygd) {
                antallDager = Dekningsgrad._80.equals(dekningsgrad) ? MINSTEDAGER_UFØRE_80_PROSENT : MINSTEDAGER_UFØRE_100_PROSENT;
            }
            if (minsterettFarMedmor) {
                builder.minsterettDager(antallDager);
                builder.farUttakRundtFødselDager(UTTAK_RUNDT_FØDSEL_DAGER);
            } else {
                builder.utenAktivitetskravDager(antallDager);
            }
        }
        return builder;
    }
}
