package no.nav.foreldrepenger.ytelse.beregning.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.UttakResultatRepoMapper;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultat;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class UttakResultatMapper implements UttakResultatRepoMapper {

    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private MapUttakResultatFraVLTilRegel mapper;

    UttakResultatMapper() {
        //for proxy
    }

    @Inject
    public UttakResultatMapper(SvangerskapspengerUttakResultatRepository uttakResultatRepository, MapUttakResultatFraVLTilRegel mapUttakResultatFraVLTilRegelSVP) {
        this.svangerskapspengerUttakResultatRepository = uttakResultatRepository;
        this.mapper = mapUttakResultatFraVLTilRegelSVP;
    }

    @Override
    public UttakResultat hentOgMapUttakResultat(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var uttakResultat = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(ref.behandlingId())
            .orElseThrow(() -> new IllegalStateException("Mangler uttaksresultat for svangerskapspenger-behandling i beregn ytelse."));
        return mapper.mapFra(uttakResultat, input);
    }
}
