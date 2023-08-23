package no.nav.foreldrepenger.web.app.tjenester.registrering.es;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles;
import no.nav.vedtak.felles.xml.soeknad.engangsstoenad.v3.Engangsstønad;
import no.nav.vedtak.felles.xml.soeknad.engangsstoenad.v3.ObjectFactory;
import no.nav.vedtak.felles.xml.soeknad.v3.OmYtelse;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;

import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles.mapAnnenForelder;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles.mapRelasjonTilBarnet;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@BehandlingTypeRef
@ApplicationScoped
public class YtelseSøknadMapper implements SøknadMapper {

    private PersoninfoAdapter personinfoAdapter;

    protected YtelseSøknadMapper() {
    }

    @Inject
    public YtelseSøknadMapper(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    @Override
    public <V extends ManuellRegistreringDto> Soeknad mapSøknad(V registreringDto, NavBruker navBruker) {
        var søknad = SøknadMapperFelles.mapSøknad(registreringDto, navBruker);
        var engangsstønad = new Engangsstønad();
        var søkersRelasjonTilBarnet = mapRelasjonTilBarnet(registreringDto);
        engangsstønad.setSoekersRelasjonTilBarnet(søkersRelasjonTilBarnet);
        engangsstønad.setMedlemskap(SøknadMapperFelles.mapMedlemskap(registreringDto));
        engangsstønad.setAnnenForelder(mapAnnenForelder(registreringDto, personinfoAdapter));

        søknad.setOmYtelse(mapOmYtelse(engangsstønad));
        return søknad;
    }

    public OmYtelse mapOmYtelse(Engangsstønad ytelse) {
        var omYtelse = new OmYtelse();
        omYtelse.getAny().add(new ObjectFactory().createEngangsstønad(ytelse));
        return omYtelse;
    }

}
