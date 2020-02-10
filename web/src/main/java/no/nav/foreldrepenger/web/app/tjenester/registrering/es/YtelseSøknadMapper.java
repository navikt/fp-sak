package no.nav.foreldrepenger.web.app.tjenester.registrering.es;

import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles.mapAnnenForelder;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles.mapRelasjonTilBarnet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles;
import no.nav.vedtak.felles.xml.soeknad.engangsstoenad.v3.Engangsstønad;
import no.nav.vedtak.felles.xml.soeknad.engangsstoenad.v3.ObjectFactory;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.SoekersRelasjonTilBarnet;
import no.nav.vedtak.felles.xml.soeknad.v3.OmYtelse;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;

@FagsakYtelseTypeRef("ES")
@BehandlingTypeRef
@ApplicationScoped
public class YtelseSøknadMapper implements SøknadMapper {

    private TpsTjeneste tpsTjeneste;

    protected YtelseSøknadMapper() {
    }

    @Inject
    public YtelseSøknadMapper(TpsTjeneste tpsTjeneste) {
        this.tpsTjeneste = tpsTjeneste;
    }

    @Override
    public <V extends ManuellRegistreringDto> Soeknad mapSøknad(V registreringDto, NavBruker navBruker) {
        Soeknad søknad = SøknadMapperFelles.mapSøknad(registreringDto, navBruker);
        Engangsstønad engangsstønad = new Engangsstønad();
        SoekersRelasjonTilBarnet søkersRelasjonTilBarnet = mapRelasjonTilBarnet(registreringDto);
        engangsstønad.setSoekersRelasjonTilBarnet(søkersRelasjonTilBarnet);
        engangsstønad.setMedlemskap(SøknadMapperFelles.mapMedlemskap(registreringDto));
        engangsstønad.setAnnenForelder(mapAnnenForelder(registreringDto, tpsTjeneste));

        søknad.setOmYtelse(mapOmYtelse(engangsstønad));
        return søknad;
    }

    public OmYtelse mapOmYtelse(Engangsstønad ytelse) {
        OmYtelse omYtelse = new OmYtelse();
        omYtelse.getAny().add(new ObjectFactory().createEngangsstønad(ytelse));
        return omYtelse;
    }

}
