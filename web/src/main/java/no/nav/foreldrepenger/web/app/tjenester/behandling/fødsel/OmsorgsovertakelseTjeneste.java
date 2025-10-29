package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.OmsorgsovertakelseVilkårTypeUtleder;
import no.nav.foreldrepenger.familiehendelse.kontrollerfakta.sammebarn.YtelserSammeBarnTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.Kilde;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.OmsorgsovertakelseDto;

@ApplicationScoped
public class OmsorgsovertakelseTjeneste {

    private static final Map<FagsakYtelseType, List<OmsorgsovertakelseVilkårType>> YTELSE_DELVILKÅR = Map.of(
        FagsakYtelseType.ENGANGSTØNAD, List.of(OmsorgsovertakelseVilkårType.ES_ADOPSJONSVILKÅRET, OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD,
            OmsorgsovertakelseVilkårType.ES_OMSORGSVILKÅRET, OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_4_LEDD),
        FagsakYtelseType.FORELDREPENGER, List.of(OmsorgsovertakelseVilkårType.FP_ADOPSJONSVILKÅRET,
            OmsorgsovertakelseVilkårType.FP_FORELDREANSVARSVILKÅRET_2_LEDD, OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET)
    );

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlingRepository behandlingRepository;
    private OmsorgsovertakelseVilkårTypeUtleder delvilkårUtleder;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private YtelserSammeBarnTjeneste ytelserSammeBarnTjeneste;

    OmsorgsovertakelseTjeneste() {
        // For CDI proxy
    }

    @Inject
    public OmsorgsovertakelseTjeneste(FamilieHendelseTjeneste familieHendelseTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      OmsorgsovertakelseVilkårTypeUtleder delvilkårUtleder,
                                      BehandlingsresultatRepository behandlingsresultatRepository,
                                      YtelserSammeBarnTjeneste ytelserSammeBarnTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.delvilkårUtleder = delvilkårUtleder;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.ytelserSammeBarnTjeneste = ytelserSammeBarnTjeneste;
    }

    public OmsorgsovertakelseDto hentOmsorgsovertakelse(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var familieHendelse = familieHendelseTjeneste.hentAggregat(behandlingId);

        var søknadData = lagOmsorgsovertakelse(familieHendelse.getSøknadVersjon(), ref);

        var registerData = new OmsorgsovertakelseDto.Register(familieHendelse.getBekreftetVersjon().map(this::getBarn).orElseGet(Collections::emptyList));

        var gjeldendeKilde = familieHendelse.getOverstyrtVersjon().isPresent() ? Kilde.SAKSBEHANDLER : Kilde.SØKNAD;
        var gjeldendeData = familieHendelse.getOverstyrtVersjon()
            .map(overstyrt -> lagOmsorgsovertakelse(overstyrt, ref))
            .orElse(søknadData);

        var aktuelleAvslagsårsaker = YTELSE_DELVILKÅR.getOrDefault(behandling.getFagsakYtelseType(), List.of()).stream()
            .collect(Collectors.toMap(Function.identity(), OmsorgsovertakelseVilkårType::getAvslagsårsaker));
        var tidligereValg = behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .map(Behandlingsresultat::getVilkårResultat)
            .map(VilkårResultat::getVilkårene).orElseGet(List::of).stream()
            .filter(v -> VilkårType.OMSORGSOVERTAKELSEVILKÅR.equals(v.getVilkårType()))
            .filter(v -> v.getAvslagsårsak() != null || VilkårUtfallType.erFastsatt(v.getGjeldendeVilkårUtfall()))
            .findFirst()
            .map(v -> new OmsorgsovertakelseDto.SaksbehandlerVurdering(v.getGjeldendeVilkårUtfall(), v.getAvslagsårsak()))
            .orElse(null);
        var andresaker = ytelserSammeBarnTjeneste.andreSakerMedSammeFamilieHendelse(ref).stream()
            .map(snr -> new OmsorgsovertakelseDto.Saksnummer(snr.getVerdi()))
            .toList();

        return new OmsorgsovertakelseDto(søknadData, registerData, gjeldendeKilde, gjeldendeData, andresaker, tidligereValg, aktuelleAvslagsårsaker);
    }

    private OmsorgsovertakelseDto.Omsorgsovertakelse lagOmsorgsovertakelse(FamilieHendelseEntitet familieHendelse, BehandlingReferanse ref) {
        return new OmsorgsovertakelseDto.Omsorgsovertakelse(getBarn(familieHendelse),
            familieHendelse.getAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato).orElse(null),
            familieHendelse.getAntallBarn(),
            delvilkårUtleder.utledDelvilkår(ref, familieHendelse),
            familieHendelse.getAdopsjon().map(AdopsjonEntitet::getErEktefellesBarn).orElse(null),
            familieHendelse.getAdopsjon().map(AdopsjonEntitet::getAnkomstNorgeDato).orElse(null));
    }

    private List<OmsorgsovertakelseDto.BarnHendelseData> getBarn(FamilieHendelseEntitet familieHendelse) {
        return Optional.ofNullable(familieHendelse).map(FamilieHendelseEntitet::getBarna).orElseGet(List::of).stream()
            .sorted(UidentifisertBarn.FØDSEL_COMPARATOR)
            .map(barn -> new OmsorgsovertakelseDto.BarnHendelseData(barn.getFødselsdato(), barn.getDødsdato().orElse(null),
                barn.getBarnNummer()))
            .toList();
    }

}
