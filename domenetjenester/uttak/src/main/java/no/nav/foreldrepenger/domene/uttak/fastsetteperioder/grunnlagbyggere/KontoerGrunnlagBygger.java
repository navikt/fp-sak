package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.time.Period;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Konto;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Kontoer;

@ApplicationScoped
public class KontoerGrunnlagBygger {

    // TODO (TFP-4846) flytt til Stønadskontoberegning og lagre på FR el Fagsak
    private static final int MINSTEDAGER_UFØRE_100_PROSENT = 75;
    private static final int MINSTEDAGER_UFØRE_80_PROSENT = 95;

    private static final int MOR_TO_TETTE_MINSTERETT_DAGER = 110;
    private static final int FAR_TO_TETTE_MINSTERETT_DAGER = 40;
    private static final int BFHR_MINSTERETT_DAGER = 40;
    private static final int UTTAK_RUNDT_FØDSEL_DAGER = 10;

    private static final int TO_TETTE_UKER_MELLOM_FAMHENDELSE = 48;


    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FamilieHendelseRepository familieHendelseRepository;

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

    public static Kontoer.Builder byggKunRettighetFarUttakRundtFødsel(BehandlingReferanse ref) {
        if (ref.getSkjæringstidspunkt().utenMinsterett()) {
            return new Kontoer.Builder();
        } else {
            return new Kontoer.Builder().farUttakRundtFødselDager(UTTAK_RUNDT_FØDSEL_DAGER);
        }
    }

    private Konto.Builder map(Stønadskonto stønadskonto) {
        return new Konto.Builder()
            .trekkdager(stønadskonto.getMaxDager())
            .type(UttakEnumMapper.map(stønadskonto.getStønadskontoType()));
    }

    private Set<Stønadskonto> hentStønadskontoer(BehandlingReferanse ref) {
        return fagsakRelasjonRepository.finnRelasjonFor(ref.saksnummer()).getGjeldendeStønadskontoberegning()
            .orElseThrow(() -> new IllegalArgumentException("Behandling mangler stønadskontoer"))
            .getStønadskontoer();
    }

    /*
     * TFP-4846 legge inn regler for minsterett i stønadskontoutregningen + bruke standardkonfigurasjon
     */
    private Kontoer.Builder getBuilder(BehandlingReferanse ref, ForeldrepengerGrunnlag foreldrepengerGrunnlag, Set<Stønadskonto> stønadskontoer) {
        var builder = new Kontoer.Builder();
        var erMor = RelasjonsRolleType.MORA.equals(ref.relasjonRolle());
        var erForeldrepenger = stønadskontoer.stream().map(Stønadskonto::getStønadskontoType).anyMatch(StønadskontoType.FORELDREPENGER::equals);
        var minsterett = !ref.getSkjæringstidspunkt().utenMinsterett();
        var totette = minsterett && toTette(foreldrepengerGrunnlag);
        var morHarUføretrygd = foreldrepengerGrunnlag.getUføretrygdGrunnlag()
            .filter(UføretrygdGrunnlagEntitet::annenForelderMottarUføretrygd)
            .isPresent();
        var flerbarnsdager = stønadskontoer.stream()
            .filter(stønadskonto -> StønadskontoType.FLERBARNSDAGER.equals(stønadskonto.getStønadskontoType()))
            .findFirst();
        flerbarnsdager.map(stønadskonto -> builder.flerbarnsdager(stønadskonto.getMaxDager()));

        if (erForeldrepenger && (minsterett || morHarUføretrygd)) {
            var dekningsgrad = fagsakRelasjonRepository.finnRelasjonFor(ref.saksnummer()).getGjeldendeDekningsgrad();
            var antallDager = 0;
            if (minsterett && totette) {
                antallDager = erMor ? MOR_TO_TETTE_MINSTERETT_DAGER : FAR_TO_TETTE_MINSTERETT_DAGER;
            }
            if (minsterett && !erMor) {
                antallDager = totette ?  Math.max(BFHR_MINSTERETT_DAGER, FAR_TO_TETTE_MINSTERETT_DAGER) : BFHR_MINSTERETT_DAGER;
            }
            if (morHarUføretrygd && !erMor) {
                antallDager = Dekningsgrad._80.equals(dekningsgrad) ? MINSTEDAGER_UFØRE_80_PROSENT : MINSTEDAGER_UFØRE_100_PROSENT;
            }
            if (minsterett) {
                builder.minsterettDager(antallDager);
                builder.farUttakRundtFødselDager(UTTAK_RUNDT_FØDSEL_DAGER);
            } else {
                builder.utenAktivitetskravDager(antallDager);
            }
        }
        return builder;
    }

    private boolean toTette(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var denneSaken = Optional.ofNullable(foreldrepengerGrunnlag.getFamilieHendelser())
            .map(FamilieHendelser::getGjeldendeFamilieHendelse)
            .map(FamilieHendelse::getFamilieHendelseDato).orElse(null);
        if (denneSaken == null) {
            return false;
        }
        var nesteSak = foreldrepengerGrunnlag.getNesteSakGrunnlag().map(NesteSakGrunnlagEntitet::getHendelsedato);
        var grenseToTette = denneSaken.plus(Period.ofWeeks(TO_TETTE_UKER_MELLOM_FAMHENDELSE)).plusDays(1);
        return nesteSak.filter(grenseToTette::isAfter).isPresent();
    }
}
