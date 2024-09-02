package no.nav.foreldrepenger.domene.uttak.svp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.svangerskapspenger.domene.felles.Arbeidsforhold;
import no.nav.svangerskapspenger.domene.søknad.Opphold;
import no.nav.svangerskapspenger.tjeneste.fastsettuttak.FastsettPerioderTjeneste;

@ApplicationScoped
public class FastsettUttaksresultatTjeneste {

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private AvklarteDatoerTjeneste avklarteDatoerTjeneste;
    private UttaksresultatMapper uttaksresultatMapper;
    private RegelmodellSøknaderMapper regelmodellSøknaderMapper;
    private InngangsvilkårSvpBygger inngangsvilkårSvpBygger;
    private OppholdTjeneste oppholdTjeneste;
    private final FastsettPerioderTjeneste fastsettPerioderTjeneste = new FastsettPerioderTjeneste();

    public FastsettUttaksresultatTjeneste() {
        // For CDI
    }

    @Inject
    public FastsettUttaksresultatTjeneste(UttakRepositoryProvider behandlingRepositoryProvider,
                                          AvklarteDatoerTjeneste avklarteDatoerTjeneste,
                                          UttaksresultatMapper uttaksresultatMapper,
                                          RegelmodellSøknaderMapper regelmodellSøknaderMapper,
                                          InngangsvilkårSvpBygger inngangsvilkårSvpBygger,
                                          OppholdTjeneste oppholdTjeneste) {
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.svangerskapspengerUttakResultatRepository = behandlingRepositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.avklarteDatoerTjeneste = avklarteDatoerTjeneste;
        this.uttaksresultatMapper = uttaksresultatMapper;
        this.regelmodellSøknaderMapper = regelmodellSøknaderMapper;
        this.inngangsvilkårSvpBygger = inngangsvilkårSvpBygger;
        this.oppholdTjeneste = oppholdTjeneste;
    }

    public SvangerskapspengerUttakResultatEntitet fastsettUttaksresultat(UttakInput input) {
        var behandlingId = input.getBehandlingReferanse().behandlingId();
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        var nyeSøknader = regelmodellSøknaderMapper.hentSøknader(input);
        var avklarteDatoer = avklarteDatoerTjeneste.finn(input);
        var inngangsvilkår = inngangsvilkårSvpBygger.byggInngangsvilårSvp(behandlingsresultat.getVilkårResultat());
        Map<Arbeidsforhold, List<Opphold>> oppholdListePerArbeidsforholdMap = new HashMap<>();
        avklarteDatoer.getFørsteLovligeUttaksdato()
            .ifPresent(dato -> oppholdListePerArbeidsforholdMap.putAll(oppholdTjeneste.finnOppholdFraTilretteleggingOgInntektsmelding(input.getBehandlingReferanse(),
                input.getSkjæringstidspunkt().orElseThrow(), input.getYtelsespesifiktGrunnlag())));


        var uttaksperioder = fastsettPerioderTjeneste.fastsettePerioder(nyeSøknader, avklarteDatoer, inngangsvilkår, oppholdListePerArbeidsforholdMap);

        var svangerskapspengerUttakResultatEntitet = uttaksresultatMapper.tilEntiteter(behandlingsresultat, uttaksperioder);
        svangerskapspengerUttakResultatRepository.lagre(behandlingId, svangerskapspengerUttakResultatEntitet);
        return svangerskapspengerUttakResultatEntitet;
    }

}
