package no.nav.foreldrepenger.web.app.tjenester.hendelser.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.FødselHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.MottattHendelseTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.FødselHendelse;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.EnkelRespons;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.ForretningshendelseRegistrerer;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.HendelserRestTjeneste;
import no.nav.vedtak.log.sporingslogg.Sporingsdata;
import no.nav.vedtak.log.sporingslogg.SporingsloggHelper;
import no.nav.vedtak.log.sporingslogg.StandardSporingsloggId;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.FØDSEL)
public class FødselForretningshendelseRegistrerer implements ForretningshendelseRegistrerer<FødselHendelseDto> {

    private static final String CREATE_ACTION = "create";

    private MottattHendelseTjeneste mottattHendelseTjeneste;

    FødselForretningshendelseRegistrerer() {
        //CDI
    }

    @Inject
    public FødselForretningshendelseRegistrerer(MottattHendelseTjeneste mottattHendelseTjeneste) {
        this.mottattHendelseTjeneste = mottattHendelseTjeneste;
    }

    @Override
    public EnkelRespons registrer(FødselHendelseDto hendelseDto) {
        List<String> aktørIdListe = hendelseDto.getAktørIdForeldre().stream().map(AktørId::new).map(AktørId::getId).collect(Collectors.toList());
        FødselHendelse hendelse = new FødselHendelse(aktørIdListe, hendelseDto.getFødselsdato());
        mottattHendelseTjeneste.registrerHendelse(hendelseDto.getId(), hendelse);
        loggSporingsdata(hendelseDto);
        return new EnkelRespons("OK");
    }

    private static void loggSporingsdata(FødselHendelseDto dto) {
        String endepunkt = HendelserRestTjeneste.class.getAnnotation(Path.class).value() + "/fodsel";
        dto.getAktørIdForeldre().stream().forEach(aktørIdForelder -> {
            Sporingsdata sd = Sporingsdata.opprett(endepunkt).leggTilId(StandardSporingsloggId.AKTOR_ID, aktørIdForelder);
            SporingsloggHelper.logSporing(HendelserRestTjeneste.class, sd, CREATE_ACTION, endepunkt);
        });
    }
}
