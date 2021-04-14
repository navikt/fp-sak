package no.nav.foreldrepenger.ytelse.beregning.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.UttakResultatRepoMapper;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class UttakResultatMapper implements UttakResultatRepoMapper {

    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private MapUttakResultatFraVLTilRegel mapper;

    UttakResultatMapper() {
        //for proxy
    }

    @Inject
    public UttakResultatMapper(BehandlingRepositoryProvider repositoryProvider, MapUttakResultatFraVLTilRegel mapUttakResultatFraVLTilRegelSVP) {
        this.svangerskapspengerUttakResultatRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.mapper = mapUttakResultatFraVLTilRegelSVP;
    }

    @Override
    public UttakResultat hentOgMapUttakResultat(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var uttakResultat = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(ref.getBehandlingId())
            .orElseThrow(() -> new IllegalStateException("Mangler uttaksresultat for svangerskapspenger-behandling i beregn ytelse."));
        return mapper.mapFra(uttakResultat, input);
    }
}
