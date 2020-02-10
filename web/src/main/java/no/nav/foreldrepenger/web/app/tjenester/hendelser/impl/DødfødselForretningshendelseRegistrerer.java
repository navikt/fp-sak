package no.nav.foreldrepenger.web.app.tjenester.hendelser.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.DødfødselHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.MottattHendelseTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.DødfødselHendelse;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.EnkelRespons;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.ForretningshendelseRegistrerer;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.HendelserRestTjeneste;
import no.nav.vedtak.log.sporingslogg.Sporingsdata;
import no.nav.vedtak.log.sporingslogg.SporingsloggHelper;
import no.nav.vedtak.log.sporingslogg.StandardSporingsloggId;

@ApplicationScoped
@HendelseTypeRef(HendelseTypeRef.DØDFØDSEL)
public class DødfødselForretningshendelseRegistrerer implements ForretningshendelseRegistrerer<DødfødselHendelseDto> {

    private static final String CREATE_ACTION = "create";

    private MottattHendelseTjeneste mottattHendelseTjeneste;

    DødfødselForretningshendelseRegistrerer() {
        //CDI
    }

    @Inject
    public DødfødselForretningshendelseRegistrerer(MottattHendelseTjeneste mottattHendelseTjeneste) {
        this.mottattHendelseTjeneste = mottattHendelseTjeneste;
    }

    @Override
    public EnkelRespons registrer(DødfødselHendelseDto hendelseDto) {
        List<String> aktørIdListe = hendelseDto.getAktørId().stream().map(AktørId::new).map(AktørId::getId).collect(Collectors.toList());
        DødfødselHendelse hendelse = new DødfødselHendelse(aktørIdListe, hendelseDto.getDødfødselsdato());
        mottattHendelseTjeneste.registrerHendelse(hendelseDto.getId(), hendelse);
        loggSporingsdata(hendelseDto);
        return new EnkelRespons("OK");
    }

    private static void loggSporingsdata(DødfødselHendelseDto dto) {
        String endepunkt = HendelserRestTjeneste.class.getAnnotation(Path.class).value() + "/dodfodsel";
        dto.getAktørId().stream().forEach(aktørId -> {
            Sporingsdata sd = Sporingsdata.opprett(endepunkt).leggTilId(StandardSporingsloggId.AKTOR_ID, aktørId);
            SporingsloggHelper.logSporing(HendelserRestTjeneste.class, sd, CREATE_ACTION, endepunkt);
        });
    }
}
