double proc _roundBy(number @in, number @by)
{
  double @in_ = double(@in);
  double @by_ = double(@by);

  return(double(floor(@in_ * @by_) / @by_));
}

double proc _round(number @in)
{
  return(_roundBy(@in, 2.0));
  # return(double(floor(@in * 100.0) / 100.0));
}

void proc _updateFluxSign(array @shop)
{
  array @location = @shop['location']
  number @newValue = @shop['value']


  auto @priceCreate = _roundBy(@newValue * @shop['stackSize'] * 2.0, 4.0);
  auto @priceDestroy = _roundBy(@newValue * @shop['stackSize'] / 2.0, 4.0);

  int @fill = integer(@shop['stock'] * 100.0 / @shop['maxCapacity'] + 0.5);

  string @stringCreate = 'B '.string(@priceCreate);
  string @stringDestroy = 'S '.string(@priceDestroy);


  if(@fill == 100)
  { @stringDestroy = colorize('&ofull&r'); }

  if(@fill == 0)
  { @stringCreate = colorize('&oempty&r'); }

  string @term = colorize('&o');

  array @text = array(
    'AdminShop'.@term,
    string(@shop['stackSize']).@term,
    @stringCreate.' : '.@stringDestroy.@term,
    _pretty_name(@shop['display']).@term,
  );

  set_sign_text(@location, @text);
}

void proc _shopRestock(array @shop, int @epoch)
{
  int @half_cap = @shop['maxCapacity'] / 2;

  int @regen_amt = integer((@epoch - @shop['lastUpdate']) / 600000.0 * @half_cap + 0.5);
  int @stock_diff = (@half_cap - @shop['stock']);

  if(@stock_diff > 0)
  {
    @shop['stock'] = min(@half_cap, @shop['stock'] + @regen_amt);
  }
  else
  {
    @shop['stock'] = max(@half_cap, @shop['stock'] - @regen_amt);
  }

  @shop['lastUpdate'] = @epoch;
  _updateFluxSign(@shop);
}

void proc _shopRevalue(array @shop)
{
  double @delta = double((@shop['createdDT'] - @shop['destroyedDT']) / @shop['maxCapacity'] / 4.0);

  double @factor = double(2.0 ** @delta);
  @factor = double(max(min(@factor, 4.0), 0.25));

  @shop['value'] = max(@factor * @shop['value'] * @shop['stackSize'], 0.5) / @shop['stackSize'];

  @shop['createdDT'] = 0;
  @shop['destroyedDT'] = 0;

  _updateFluxSign(@shop);
}

void proc _shopAccess(string @player, int @shopIndex, string @action)
{
  array @config = _get_meta('config');
  boolean @buying = false;

  if(@action == to_lower(@config['buy_action']))
  { @buying = true; }

  array @shops = _getPerm('shops');

  if(!array_index_exists(@shops, @shopIndex))
  {
    _message_client(@player, '&cFluxshop error, please notify an admin! Transaction cancelled');
    return();
  }

  array @shop = @shops[@shopIndex];

  if(!@shop['enabled'])
  {
    _message_client(@player, '&cSorry, this shop is currently disabled! Try again later')
    return();
  }
  auto @shopValue = @shop['value'];
  auto @priceCreate = _roundBy(@shopValue * @shop['stackSize'] * 2.0, 4.0);
  auto @priceDestroy = _roundBy(@shopValue * @shop['stackSize'] / 2.0, 4.0);
  string @item = @shop['item'];
  string @display = _pretty_name(@shop['display']);

  double @value;
  double @balance = 0.0;
  try(
    @balance = acc_balance(@player);
  ,@ex,
  );

  if(@buying)
  {
    if(@shop['stock'] <= 0)
    {
      _message_client(@player, '&cSorry, this shop is currently out of stock! Try again later')
      return();
    }

    if(@priceCreate > @balance)
    {
      _message_client(@player, '&cSorry, you don\'t have enough money!')
      return();
    }

    int @createQty = min(min(@shop['stackSize'], floor(@balance / @priceCreate)), @shop['stock']);

    int @actualQty = @createQty - pgive_item(@player, @item, @createQty);

    if(@actualQty == 0)
    {
      _message_client(@player, '&cYour inventory is full!')
      return();
    }

    @value = _roundBy(@createQty * @priceCreate / @shop['stackSize'], 100.0);

    try(
      acc_set(@player, @balance - @value)
    ,@ex,
      ptake_item(@player, @item, @actualQty);
      _message_client(@player, '&cCould not update your balance, please contact an admin! Transaction cancelled')
      return();
    );

	_message_client('~console', '[FluxShop] '.@player.' bought '.string(@actualQty).' '.@display.' for '.@value)
    _message_client('&aYou bought '.string(@actualQty).' '.@display.' for '.@value.'r');
    @shop['createdDT'] += @actualQty;
    @shop['stock'] -= @actualQty;
  }
  else
  {
    if(@shop['stock'] >= @shop['maxCapacity'])
    {
      _message_client(@player, '&cSorry, this shop is currently out of room! Try again later')
      return();
    }

    if(@priceDestroy < 0.01)
    {
      _message_client(@player, '&cSorry, this shop is not currently buying!')
      return();
    }

    int @has = phas_item(@player, @item);
    if(@has == 0)
    {
      _message_client(@player, '&cSorry, you don\'t have any '.@display.'!');
      return();
    }

    int @destroyQty = min(min(@shop['stackSize'], @shop['maxCapacity'] - @shop['stock']), @has);

    int @actualQty = ptake_item(@player, @item, @destroyQty);
    @value = _roundBy(@destroyQty * @priceDestroy / @shop['stackSize'], 100.0);

    try(
      acc_set(@player, @balance + @value);
    ,@ex,
      pgive_item(@player, @item, @actualQty);
      _message_client(@player, '&cCould not update your balance, please contact an admin! Transaction cancelled')
      return();
    );

	_message_client('~console', '[FluxShop] '.@player.' sold '.string(@actualQty).' '.@display.' for '.@value)
    _message_client('&aYou sold '.string(@actualQty).' '.@display.' for '.@value.'r');
    @shop['destroyedDT'] += @actualQty;
    @shop['stock'] += @actualQty;
  }

  # Modified by transaction
  # @shop['createdDT'] #
  # @shop['destroyedDT']
  # @shop['stock']

  # Modified by update
  # @shop['value'] # Current value, *4 is buy, /4 is sell

  # Modified by command
  # @shop['enabled']
  # @shop['stackSize']
  # @shop['maxCapacity']
  # @shop['location']
  # @shop['item']
  # @shop['display']
  _updateFluxSign(@shop);
  _setPerm('shops', @shops);
}

boolean proc _executor(string @alias, string @sender, array @parameters, array @info)
{
  if(array_size(@parameters) == 0)
  {
    _message_client(@sender, 'Usage: /fluxshop <create|info|modify|remove>');
    return(true);
  }

  string @arg = @parameters[0];
  array @shops = _getPerm('shops');
  if(is_null(@shops))
  { @shops = array(); }

  switch(@arg)
  {
    case 'create':
      if(array_size(@parameters) < 3 || !is_integral(@parameters[1]) || !is_numeric(@parameters[2]))
      {
        _message_client(@sender, 'Usage: /fluxshop create <capacity:int> <price:float>');
        return(true);
      }

      array @item_arr = pinv(@sender, null);
      if(is_null(@item_arr) || @item_arr['type'] == 0)
      {
        _message_client(@sender, '&cPlease run this command while holding the item you wish to sell (including stack size)');
        return(true);
      }

      array @location = null;
      try(
        @location = pcursor(@sender)

        array @text = get_sign_text(@location);
        array_remove_values(@text, '');

        if(array_size(@text) > 0)
        { @location = null; }

        ,@ex,
        @location = null
      );

      if(is_null(@location))
      {
        _message_client(@sender, '&cPlease aim at an empty sign and run this command to create a shop');
        return(true);
      }

      foreach(@shop in @shops)
      {
        @loc = @shop['location'];

        if(@loc[0] == @location[0] && @loc[2] == @location[2]
        && @loc[1] == @location[1] && @loc[3] == @location[3])
        {
          _message_client(@sender, '&cThere is already a fluxshop at that location!');
          return(true);
        }
      }

      int @count = @item_arr['qty'];
      int @capacity = integer(@parameters[1]);
      double @price = double(@parameters[2] / @count);

      string @item = string(@item_arr['type']).':'.string(@item_arr['data']);
      string @display = data_name(@item_arr);

      array @new_shop = array(
        'maxCapacity': @capacity * 2,
        'createdDT':   0,
        'destroyedDT': 0,
        'display':     @display,
        'enabled':     true,
        'item':        @item,
        'location':    @location,
        'stackSize':   @count,
        'stock':       @capacity,
        'value':       @price,
        'lastUpdate':  time(),
      );

      @shops[] = @new_shop;

      _updateFluxSign(@new_shop);
      _setPerm('shops', @shops);
      _message_client('&aShop created!');

    case 'info':
      array @location = null;
      try(
        @location = pcursor(@sender)
        ,@ex,
      );

      if(is_null(@location))
      {
        _message_client(@sender, '&cNot a fluxshop!');
        return(true);
      }

      foreach(@index: @shop in @shops)
      {
        @loc = @shop['location'];

        if(@loc[0] == @location[0] && @loc[2] == @location[2]
        && @loc[1] == @location[1] && @loc[3] == @location[3])
        {
          msg(@shop);
          return(true);
        }
      }
    case 'modify':
      array @location = null;
      try(
        @location = pcursor(@sender)
        ,@ex,
      );

      if(is_null(@location))
      {
        _message_client(@sender, '&cPlease look at the fluxshop you want to modify');
        return(true);
      }

      @amt = 0;
      if(array_size(@parameters) > 1 && is_integral(@parameters[1]))
      { @amt = @parameters[1] * 2; }

      foreach(@index: @shop in @shops)
      {
        @loc = @shop['location'];

        if(@loc[0] == @location[0] && @loc[2] == @location[2]
        && @loc[1] == @location[1] && @loc[3] == @location[3])
        {
          if(@amt > 0)
          {
            @shop['maxCapacity'] = @amt;

            _setPerm('shops', @shops);
            _message_client('&aShop updated!');
          }
          else
          { _message_client('&cCapacity must be greater than 0'); }
          return(true);
        }
      }

    case 'refresh':
      array @location = null;
      try(
        @location = pcursor(@sender)
        ,@ex,
      );

      if(is_null(@location))
      {
        _message_client(@sender, '&cPlease look at the fluxshop you want to refresh');
        return(true);
      }

      @amt = 0;
      if(array_size(@parameters) > 1 && is_integral(@parameters[1]))
      { @amt = @parameters[1]; }

      foreach(@index: @shop in @shops)
      {
        @loc = @shop['location'];

        if(@loc[0] == @location[0] && @loc[2] == @location[2]
        && @loc[1] == @location[1] && @loc[3] == @location[3])
        {
          if(@amt < 0)
          { @shop['destroyedDT'] += -@amt; }
          else
          { @shop['createdDT'] += @amt; }
          _shopRevalue(@shop);
          _setPerm('shops', @shops);
          _message_client('&aShop updated!');
          return(true);
        }
      }

    case 'remove':
      array @location = null;
      try(
        @location = pcursor(@sender)
        ,@ex,
      );

      if(is_null(@location))
      {
        _message_client(@sender, '&cPlease look at the fluxshop you want to remove');
        return(true);
      }

      foreach(@index: @shop in @shops)
      {
        @loc = @shop['location'];

        if(@loc[0] == @location[0] && @loc[2] == @location[2]
        && @loc[1] == @location[1] && @loc[3] == @location[3])
        {
          array_remove(@shops, @index);
          set_sign_text(@loc, array('','','',''))
          _setPerm('shops', @shops);
          _message_client('&aShop removed!');
          return(true);
        }
      }

      _message_client('&cThat is not a fluxshop');
      return(true);
  }

  return(true);
}

array proc _completor(string @alias, string @sender, array @parameters, array @info)
{
  if(array_size(@parameters) == 1)
  { return(_partial_array(array('create', 'info', 'modify', 'remove'), @parameters[0])); }
  else if(to_lower(@parameters[0]) == 'modify')
  { return(_partial_array(array('capacity', 'count', 'enabled', 'item', 'price'), @parameters[1])); }

  return(array());
}

array proc _details()
{
  return(array(
    'description'  : 'Control for fluctuating shops',
    'usage'        : 'Usage: /fluxshop <create|info|modify|remove>',
    'permission'   : 'shadecrest.fluxshop',
    'noPermMessage': 'No permission for /fluxshop',
    'aliases'      : array(),
  ));
}

array proc _config()
{
  return(array(
    'buy_action': 'right_click_block',

  ));
}

array proc _bindings()
{
  array @ids = array();

  @ids[] = bind('player_interact', null, array('block': data_values('wall_sign')), @event)
  {
    string @oldScript = _setScript('fluxshop');
    array @shops = _getPerm('shops');

    array @location = @event['location']

    foreach(@index: @shop in @shops)
    {
      @loc = @shop['location'];

      if(@loc[0] == @location[0] && @loc[2] == @location[2]
      && @loc[1] == @location[1] && @loc[3] == @location[3])
      {
        _shopAccess(@event['player'], @index, @event['action']);
        cancel();
        break();
      }
    }

    _setScript(@oldScript);
  }

  # Regen timer
  @ids[] = set_interval(15*1000, iclosure()
  {
    string @oldScript = _setScript('fluxshop');
    array @shops = _getPerm('shops');

    int @epoch = time();

    foreach(@shop in @shops)
    {
      _shopRestock(@shop, @epoch);
    }

    _setPerm('shops', @shops);

    _setScript(@oldScript);
  });

  # Price adjust timer
  @ids[] = set_interval(60*60*1000, iclosure()
  {
    string @oldScript = _setScript('fluxshop');
    array @shops = _getPerm('shops');

    foreach(@shop in @shops)
    {
      _shopRevalue(@shop);
    }

    _setPerm('shops', @shops);

    _setScript(@oldScript);
  });

  return(@ids);
}
