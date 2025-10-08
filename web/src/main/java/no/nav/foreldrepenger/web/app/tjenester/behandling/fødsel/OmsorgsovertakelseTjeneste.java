package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
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
    private SøknadRepository søknadRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    OmsorgsovertakelseTjeneste() {
        // For CDI proxy
    }

    @Inject
    public OmsorgsovertakelseTjeneste(FamilieHendelseTjeneste familieHendelseTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      SøknadRepository søknadRepository,
                                      VilkårResultatRepository vilkårResultatRepository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.søknadRepository = søknadRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public OmsorgsovertakelseDto hentOmsorgsovertakelse(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var farSøkerType = søknadRepository.hentSøknadHvisEksisterer(behandlingId).map(SøknadEntitet::getFarSøkerType).orElse(FarSøkerType.UDEFINERT);
        var familieHendelse = familieHendelseTjeneste.hentAggregat(behandlingId);

        var søknadData = lagOmsorgsovertakelse(familieHendelse.getSøknadVersjon(), behandling.getFagsakYtelseType(), farSøkerType);

        var registerData = new OmsorgsovertakelseDto.Register(familieHendelse.getBekreftetVersjon().map(this::getBarn).orElseGet(Collections::emptyList));

        var gjeldendeKilde = familieHendelse.getOverstyrtVersjon().isPresent() ? Kilde.SAKSBEHANDLER : Kilde.SØKNAD;
        var gjeldendeData = familieHendelse.getOverstyrtVersjon()
            .map(overstyrt -> lagOmsorgsovertakelse(overstyrt, behandling.getFagsakYtelseType(), farSøkerType))
            .orElse(søknadData);

        var aktuelleVilkår = YTELSE_DELVILKÅR.getOrDefault(behandling.getFagsakYtelseType(), List.of());
        var aktuelleAvslagsårsaker = aktuelleVilkår.stream()
            .collect(Collectors.toMap(Function.identity(), OmsorgsovertakelseVilkårType::getAvslagsårsaker));
        var tidligereValg = vilkårResultatRepository.hentHvisEksisterer(behandlingId).map(VilkårResultat::getVilkårene).orElseGet(List::of).stream()
            .filter(v -> VilkårType.OMSORGSOVERTAKELSEVILKÅR.equals(v.getVilkårType()))
            .filter(v -> v.getAvslagsårsak() != null || VilkårUtfallType.erFastsatt(v.getGjeldendeVilkårUtfall()))
            .findFirst()
            .map(v -> new OmsorgsovertakelseDto.SaksbehandlerVurdering(v.getGjeldendeVilkårUtfall(), v.getAvslagsårsak()))
            .orElse(null);

        return new OmsorgsovertakelseDto(søknadData, registerData, gjeldendeKilde, gjeldendeData, tidligereValg, aktuelleVilkår, aktuelleAvslagsårsaker);
    }

    private OmsorgsovertakelseDto.Omsorgsovertakelse lagOmsorgsovertakelse(FamilieHendelseEntitet familieHendelse, FagsakYtelseType ytelseType, FarSøkerType farSøkerType) {
        return new OmsorgsovertakelseDto.Omsorgsovertakelse(getBarn(familieHendelse),
            familieHendelse.getAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato).orElse(null),
            familieHendelse.getAntallBarn(),
            utledDelvilkår(ytelseType, familieHendelse, farSøkerType),
            familieHendelse.getAdopsjon().map(AdopsjonEntitet::isStebarnsadopsjon).orElse(null),
            familieHendelse.getAdopsjon().map(AdopsjonEntitet::getAnkomstNorgeDato).orElse(null));
    }

    private static <T> int getBarnNummer(T barn, List<T> barnListe) {
        return barnListe.indexOf(barn) + 1;
    }

    private List<OmsorgsovertakelseDto.BarnHendelseData> getBarn(FamilieHendelseEntitet familieHendelse) {
        return Optional.ofNullable(familieHendelse).map(FamilieHendelseEntitet::getBarna).orElseGet(List::of).stream()
            .map(barn -> new OmsorgsovertakelseDto.BarnHendelseData(barn.getFødselsdato(), barn.getDødsdato().orElse(null),
                getBarnNummer(barn, familieHendelse.getBarna())))
            .toList();
    }

    private static OmsorgsovertakelseVilkårType utledDelvilkår(FagsakYtelseType ytelseType, FamilieHendelseEntitet familieHendelse, FarSøkerType farSøkerType) {
        if (familieHendelse == null || familieHendelse.getAdopsjon().isEmpty()) {
            return null;
        }
        var adopsjon = familieHendelse.getAdopsjon().orElseThrow();
        if (adopsjon.getOmsorgovertakelseVilkår() != null && !OmsorgsovertakelseVilkårType.UDEFINERT.equals(adopsjon.getOmsorgovertakelseVilkår())) {
            return adopsjon.getOmsorgovertakelseVilkår();
        }
        if (FamilieHendelseType.ADOPSJON.equals(familieHendelse.getType())) {
            return switch (ytelseType) {
                case ENGANGSTØNAD -> OmsorgsovertakelseVilkårType.ES_ADOPSJONSVILKÅRET;
                case FORELDREPENGER -> adopsjon.isStebarnsadopsjon() ? OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET : OmsorgsovertakelseVilkårType.FP_ADOPSJONSVILKÅRET;
                default -> null;
            };
        } else if (FamilieHendelseType.OMSORG.equals(familieHendelse.getType()) || !FarSøkerType.UDEFINERT.equals(farSøkerType)) {
            return switch (ytelseType) {
                case ENGANGSTØNAD -> utledEngangsstønadVilkår(farSøkerType);
                case FORELDREPENGER -> OmsorgsovertakelseVilkårType.FP_FORELDREANSVARSVILKÅRET_2_LEDD;
                default -> null;
            };
        } else {
            return null;
        }
    }

    private static OmsorgsovertakelseVilkårType utledEngangsstønadVilkår(FarSøkerType farSøkerType) {
        return switch (farSøkerType) {
            case ADOPTERER_ALENE -> OmsorgsovertakelseVilkårType.ES_ADOPSJONSVILKÅRET;
            case ANDRE_FORELDER_DØD -> OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD;
            case OVERTATT_OMSORG -> OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_4_LEDD;
            case OVERTATT_OMSORG_F, ANDRE_FORELD_DØD_F -> OmsorgsovertakelseVilkårType.ES_OMSORGSVILKÅRET;
            case UDEFINERT -> null;
        };
    }
}
