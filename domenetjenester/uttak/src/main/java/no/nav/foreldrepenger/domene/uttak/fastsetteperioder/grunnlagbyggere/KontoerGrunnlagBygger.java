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

    private static final int MINSTEDAGER_100_PROSENT = 75;
    private static final int MINSTEDAGER_80_PROSENT = 95;
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
     * - TFP-4842 tillat at avslått aktivitetskrav kan innvilges fra utenAktivitetskravDager
     *
     * minsterettDager
     * - gir mulighet til å innvilge perioder selv om aktivitetskravet ikke er oppfylt
     * - automatiske trekk pga manglende søkt, avslag mv vil ikke påvirke minsterett
     * - kan utsettes og  utvide stønadsperioden
     * - TBD når kan denne brukes framfor utenAktivitetskravDager
     */
    public Kontoer.Builder byggGrunnlag(BehandlingReferanse ref, ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var stønadskontoer = hentStønadskontoer(ref);
        return new Kontoer.Builder()
            .utenAktivitetskravDager(minsterettDager(ref, foreldrepengerGrunnlag, stønadskontoer))
            .kontoList(stønadskontoer.stream().map(this::map).collect(Collectors.toList()));
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
    private int minsterettDager(BehandlingReferanse ref, ForeldrepengerGrunnlag foreldrepengerGrunnlag, Set<Stønadskonto> stønadskontoer) {
        var morHarUføretrygd = foreldrepengerGrunnlag.getUføretrygdGrunnlag()
            .filter(UføretrygdGrunnlagEntitet::annenForelderMottarUføretrygd)
            .isPresent();
        var erMor = RelasjonsRolleType.MORA.equals(ref.getRelasjonsRolleType());
        var erForeldrepenger = stønadskontoer.stream().map(Stønadskonto::getStønadskontoType).anyMatch(StønadskontoType.FORELDREPENGER::equals);
        if (morHarUføretrygd && !erMor && erForeldrepenger) {
            var dekningsgrad = fagsakRelasjonRepository.finnRelasjonFor(ref.getSaksnummer()).getGjeldendeDekningsgrad();
            return Dekningsgrad._80.equals(dekningsgrad) ? MINSTEDAGER_80_PROSENT : MINSTEDAGER_100_PROSENT;
        }
        return 0;
    }
}
