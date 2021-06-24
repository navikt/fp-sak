update PROSESS_TASK_TYPE
set kode = 'vedlikehold.adressebeskyttelse', navn = 'Oppdaget adressebeskyttelse', beskrivelse = 'Task for oppdaget adressebeskyttelse'
where kode = 'oppgavebehandling.vurderOppgaveTilbakekreving';

delete from PROSESS_TASK_TYPE
where kode in ('beregning.tilbakerullingAvSaker');
